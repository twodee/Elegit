package elegit;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Subclass of CommitTreeModel that examines local commits
 */
public class LocalCommitTreeModel extends CommitTreeModel{

    public final static String LOCAL_TREE_VIEW_NAME = "Local commit tree";

    public LocalCommitTreeModel(SessionModel model, CommitTreePanelView view){
        super(model, view);
        this.view.setName(LOCAL_TREE_VIEW_NAME);
    }

    @Override
    protected List<CommitHelper> getAllCommits(RepoHelper repoHelper) {
        return repoHelper.getAllCommits();
    }

    @Override
    protected List<BranchHelper> getAllBranches(RepoHelper repoHelper){
        return repoHelper.getBranchModel().getBranchListUntyped(BranchModel.BranchType.LOCAL);
    }
}
