package main.java.edugit;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Subclass of CommitTreeModel that examines local commits
 */
public class LocalCommitTreeModel extends CommitTreeModel{

    public LocalCommitTreeModel(SessionModel model, CommitTreePanelView view){
        super(model, view);
        this.view.setName("Local commit tree");
    }

    @Override
    protected List<CommitHelper> getAllCommits() {
        if(this.sessionModel != null && this.sessionModel.getCurrentRepoHelper() != null){
            return this.sessionModel.getCurrentRepoHelper().getLocalCommits();
        }
        return new ArrayList<>();
    }

    @Override
    protected List<CommitHelper> getNewCommits() throws GitAPIException, IOException{
        if(this.sessionModel != null && this.sessionModel.getCurrentRepoHelper() != null){
            return this.sessionModel.getCurrentRepoHelper().getNewLocalCommits();
        }
        return new ArrayList<>();
    }
}
