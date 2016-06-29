package elegit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.CannotDeleteCurrentBranchException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NotMergedException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.lib.Ref;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Model class that keeps track of all BranchModels for a RepoHelper
 *
 * Also implements git stuff that concerns multiple branches
 *      -adding, removing
 *      -tracking, untracking
 *      -merging
 *      -checkouts
 */

public class BranchModel {

    private RepoHelper repoHelper;
    private BranchHelper currentBranch;
    private List<LocalBranchHelper> localBranchesTyped;
    private List<RemoteBranchHelper> remoteBranchesTyped;

    static final Logger logger = LogManager.getLogger();

    /**
     * Constructor. Sets the repo helper and updates the local and remote branches
     *
     * @param repoHelper the repohelper to get branches for
     * @throws GitAPIException
     * @throws IOException
     */
    public BranchModel(RepoHelper repoHelper) throws GitAPIException, IOException {
        this.repoHelper = repoHelper;
        this.updateAllBranches();
    }

    /**
     * Updates local and remote branches in the model
     *
     * @throws GitAPIException
     * @throws IOException
     */
    public void updateAllBranches() throws GitAPIException, IOException {
        this.updateLocalBranches();
        this.updateRemoteBranches();
    }

    /**
     * Utilizes JGit to get a list of all local branches and refills
     * the model's list of local branches
     *
     * @throws GitAPIException
     * @throws IOException
     */
    public void updateLocalBranches() throws GitAPIException, IOException {
        List<Ref> getBranchesCall = new Git(this.repoHelper.getRepo()).branchList().call();

        this.localBranchesTyped = new ArrayList<>();
        for (Ref ref : getBranchesCall) {
            this.localBranchesTyped.add(new LocalBranchHelper(ref, this.repoHelper));
        }
    }

    /**
     * Utilizes JGit to get a list of all remote branches and refills
     * the model's list of remote branches
     *
     * @throws GitAPIException
     */
    public void updateRemoteBranches() throws GitAPIException, IOException {
        List<Ref> getBranchesCall = new Git(this.repoHelper.getRepo())
                .branchList()
                .setListMode(ListBranchCommand.ListMode.REMOTE)
                .call();

        // Rebuild the remote branches list from scratch.
        this.remoteBranchesTyped = new ArrayList<>();
        for (Ref ref : getBranchesCall) {
            // Listing the remote branches also grabs HEAD, which isn't a branch we want
            if (!ref.getName().equals("HEAD")) {
                this.remoteBranchesTyped.add(new RemoteBranchHelper(ref, this.repoHelper));
            }
        }
    }

    /**
     * Updates the current branch by checking the repository for which
     * branch is currently checked out
     *
     * @throws IOException
     */
    public void refreshCurrentBranch() throws IOException {
        String currentBranchRefString = this.repoHelper.getRepo().getFullBranch();

        for (LocalBranchHelper branch : this.localBranchesTyped) {
            if (branch.getRefPathString().equals(currentBranchRefString)) {
                this.currentBranch = branch;
                return;
            }
        }

        this.currentBranch = new LocalBranchHelper(currentBranchRefString, this.repoHelper);
    }

    /**
     * Refreshes all the head ids for all local and remote branches
     */
    public void refreshHeadIds() {
        refreshHeadIdsType(BranchType.LOCAL);
        refreshHeadIdsType(BranchType.REMOTE);
    }

    /**
     * Refreshes the head IDs for all the branches of a given type
     * @param type the type of branches to refresh
     */
    public void refreshHeadIdsType(BranchType type) {
        List<? extends BranchHelper> listToRefresh = (type == BranchType.LOCAL) ? this.localBranchesTyped : this.remoteBranchesTyped;
        for (BranchHelper branch : listToRefresh)
            if (branch.getHead() == null) {
                try {
                    branch.getHeadId();
                } catch (IOException e) {
                    logger.error("IOException getting local branches");
                    logger.debug(e.getStackTrace());
                    e.printStackTrace();
                }
            }
    }

    /**
     * Creates a local branch and tracks it
     * @param remoteBranchHelper the remote branch to track
     * @return the localBranchHelper that was added
     * @throws GitAPIException
     * @throws IOException
     * @throws RefAlreadyExistsException
     */
    public LocalBranchHelper trackRemoteBranch(RemoteBranchHelper remoteBranchHelper) throws RefAlreadyExistsException, GitAPIException, IOException {
        LocalBranchHelper tracker = this.createLocalTrackingBranchForRemote(remoteBranchHelper);
        this.localBranchesTyped.add(tracker);
        return tracker;
    }

    /**
     * Helper method that creates a local branch tracking a remote branch.
     *
     * @param remoteBranchHelper the remote branch to be tracked.
     * @return the LocalBranchHelper of the local branch tracking the given remote branch.
     * @throws GitAPIException
     * @throws IOException
     */
    private LocalBranchHelper createLocalTrackingBranchForRemote(RemoteBranchHelper remoteBranchHelper) throws GitAPIException, IOException {
        String localBranchName=this.repoHelper.getRepo().shortenRemoteBranchName(remoteBranchHelper.getRefPathString());
        Ref trackingBranchRef = new Git(this.repoHelper.getRepo()).branchCreate().
                setName(localBranchName).
                setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
                setStartPoint(remoteBranchHelper.getRefPathString()).
                call();
        LocalBranchHelper newHelper = new LocalBranchHelper(trackingBranchRef, this.repoHelper);
        this.localBranchesTyped.add(newHelper);
        return newHelper;
    }

    /**
     * Creates a new local branch using git.
     *
     * @param branchName the name of the new branch.
     * @return the new local branch's LocalBranchHelper.
     * @throws GitAPIException
     * @throws IOException
     */
    public LocalBranchHelper createNewLocalBranch(String branchName) throws GitAPIException, IOException {
        Git git = new Git(this.repoHelper.getRepo());
        Ref newBranch = git.branchCreate().setName(branchName).call();
        LocalBranchHelper newLocalBranchHelper = new LocalBranchHelper(newBranch, this.repoHelper);
        this.localBranchesTyped.add(newLocalBranchHelper);

        git.close();
        return newLocalBranchHelper;
    }

    /**
     * Deletes a local branch, but will throw exceptions if the branch is not merged
     *
     * @param localBranchToDelete the branch helper of the branch to delete
     * @throws NotMergedException
     * @throws CannotDeleteCurrentBranchException
     * @throws GitAPIException
     */
    public void deleteLocalBranch(LocalBranchHelper localBranchToDelete)
            throws NotMergedException, CannotDeleteCurrentBranchException, GitAPIException {
        Git git = new Git(this.repoHelper.getRepo());
        git.branchDelete().setBranchNames(localBranchToDelete.getRefPathString()).call();
        this.localBranchesTyped.remove(localBranchToDelete);
        git.close();
    }


    /**
     * Force deletes a branch, even if it is not merged in
     *
     * @param branchToDelete the branch helper of the branch to delete
     */
    public void forceDeleteLocalBranch(LocalBranchHelper branchToDelete) throws CannotDeleteCurrentBranchException, GitAPIException {
        Git git = new Git(this.repoHelper.getRepo());
        git.branchDelete().setForce(true).setBranchNames(branchToDelete.getRefPathString()).call();
        this.localBranchesTyped.remove(branchToDelete);
        git.close();
    }

    /**
     * Merges the current branch with the selected branch
     *
     * @param branchToMergeFrom the branch to merge into the current branch
     * @return merge result, used in determining the notification in BranchManagerController
     * @throws GitAPIException
     * @throws IOException
     */
    public MergeResult mergeWithBranch(BranchHelper branchToMergeFrom) throws GitAPIException, IOException {
        Git git = new Git(this.repoHelper.getRepo());

        MergeCommand merge = git.merge();
        merge.include(this.repoHelper.getRepo().resolve(branchToMergeFrom.getRefPathString()));

        MergeResult mergeResult = merge.call();

        // If the merge was successful, there was a new commit or fast forward, so there are unpushed
        // commits. Otherwise, repo helper doesn't need to do anything else.
        if (mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.MERGED)
                || mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.FAST_FORWARD)){
            this.repoHelper.hasUnpushedCommitsProperty.setValue(true);
        }
        git.close();

        return mergeResult;
    }

    // ************************* GETTERS AND SETTERS **************************

    /**
     * Getter for the current branch in the model
     *
     * @return the branch helper for the current branch
     */
    public BranchHelper getCurrentBranch() { return this.currentBranch; }

    /**
     * Getter for the current branch head in the model
     *
     * @return the commit helper for the head of the current branch
     */
    public CommitHelper getCurrentBranchHead() { return (this.currentBranch == null) ? null : this.currentBranch.getHead();}

    /**
     * Getter for list of branches
     *
     * @param type the type of list of branches to return
     * @return the list of branches, either all local or remote
     */
    public List<? extends BranchHelper> getBranchListTyped(BranchType type) {
        return (type==BranchType.LOCAL) ? this.localBranchesTyped : this.remoteBranchesTyped;
    }

    /**
     * Getter for local branches
     * @return list of local branches with type LocalBranchHelper
     */
    public List<LocalBranchHelper> getLocalBranchesTyped() {
        return this.localBranchesTyped;
    }

    /**
     * Getter for remote branches
     * @return list of remote branches with type RemoteBranchHelper
     */
    public List<RemoteBranchHelper> getRemoteBranchesTyped() {
        return this.remoteBranchesTyped;
    }

    /**
     * Getter for a list of branches that is typed
     * @param type the type of list to get, either local or remote
     * @return the list of branches in the 'type' list, but with type BranchHelper
     */
    public List<BranchHelper> getBranchListUntyped(BranchType type){
        List<? extends BranchHelper> typed = (type==BranchType.LOCAL) ? this.localBranchesTyped : this.remoteBranchesTyped;
        List<BranchHelper> untyped = new ArrayList<>();
        for (BranchHelper branch : typed)
            untyped.add(branch);
        return untyped;
    }

    /**
     * Getter method for all branches
     *
     * @return a list of all remote branches and local branches
     */
    public List<BranchHelper> getAllBranches() {
        List<BranchHelper> tmp = new ArrayList<>();
        for (BranchHelper branch : this.remoteBranchesTyped)
            tmp.add(branch);
        for (BranchHelper branch : this.localBranchesTyped)
            tmp.add(branch);
        return tmp;
    }

    /**
     * Gets a branch of a certain type by name
     *
     * @param type the type of the branch
     * @param branchName the name of the branch
     * @return
     */
    public BranchHelper getBranchByName(BranchType type, String branchName) {
        List<? extends BranchHelper> branchList = type==BranchType.LOCAL ? this.localBranchesTyped : this.remoteBranchesTyped;
        for (BranchHelper branch: branchList) {
            if (branch.getBranchName().equals(branchName))
                return branch;
        }
        return null;
    }

    /**
     * Checks to see if the given branch is tracked. If branch is a local
     * branch, looks to see if there is a branch in the remote branches that
     * has the same name, and vice versa. Note that the tracking is determined
     * by the config file, not just by name.
     *
     * @param branch the branch to check
     * @return true if branch is being tracked, else false
     */
    public boolean isBranchTracked(BranchHelper branch) {
        String branchName = branch.getBranchName();
        if (branch instanceof LocalBranchHelper) {
            // We can check this easily by looking at the config file, but have to first
            // check if there is an entry in the config file for LocalBranchHelper
            String merge = this.repoHelper.getRepo().getConfig().getString("branch", branchName, "merge");

            // If there is no entry in the config file for this branch, then it isn't tracked remotely
            if (merge==null) return false;

            // Otherwise there is a remote branch that the local branch is tracking remotely
            return true;
        } else {
            for (BranchHelper local : this.localBranchesTyped) {
                // Skip local branches that aren't tracked remotely, as they won't have a config entry
                if (this.repoHelper.getRepo().getConfig().getString("branch", local.getBranchName(), "merge")==null) continue;

                // Otherwise, we have to check all local branches to see if they're tracking the particular remote branch
                if (this.repoHelper.getRepo().shortenRefName(this.repoHelper.getRepo().getConfig().getString("branch", local.getBranchName(), "merge"))
                        .equals(this.repoHelper.getRepo().shortenRemoteBranchName(branch.getRefPathString()))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Updates the heads of all local and remote branches, then returns a map of them
     * @return
     */
    public Map<CommitHelper, List<BranchHelper>> getAllBranchHeads(){
        Map<CommitHelper, List<BranchHelper>> heads = new HashMap<>();

        this.refreshHeadIds();

        List<BranchHelper> branches = this.getAllBranches();

        for(BranchHelper branch : branches){
            CommitHelper head = branch.getHead();
            if(heads.containsKey(head)){
                heads.get(head).add(branch);
            }else{
                heads.put(head, Stream.of(branch).collect(Collectors.toList()));
            }
        }
        return heads;
    }

    /**
     * Gets a list of names of branches that have the given commit id as their head
     * @param commitId the commit id to look at
     * @return a list of names of branches that have the given commit id as their head
     */
    public List<String> getBranchesWithHead(String commitId) {
        return getBranchesWithHead(this.repoHelper.getCommit(commitId));
    }

    /**
     * Gets a list of names of branches that have the given commit helper as their head
     * @param commit the commit helper to look at
     * @return a list of names of branches that have the given commit helper as their head
     */
    public List<String> getBranchesWithHead(CommitHelper commit) {
        List<BranchHelper> branches = getAllBranchHeads().get(commit);
        List<String> branchLabels = new LinkedList<>();
        if(branches != null) {
            branchLabels = branches.stream()
                    .map(BranchHelper::getBranchName)
                    .collect(Collectors.toList());
        }
        return branchLabels;
    }


    /**
     * Type for branches, can be local or remote
     */
    public enum BranchType {
        LOCAL, REMOTE;
    }


}
