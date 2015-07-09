package main.java.elegit;

import main.java.elegit.treefx.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The controller class for the commit trees. Handles mouse interaction, cell selection/highlighting,
 * as well as updating the views when necessary
 */
public class CommitTreeController{

    // The list of all models controlled by this controller
    public static List<CommitTreeModel> allCommitTreeModels = new ArrayList<>();

    // The id of the currently selected cell
    private static String selectedCellID = null;

    // The session controller if this controller needs to access other models/views
    public static SessionController sessionController;

    /**
     * Takes in the cell that was clicked on, and selects it using selectCommit
     * @param cell the cell that was clicked
     */
    public static void handleMouseClicked(Cell cell){
        String id = cell.getCellId();
        if(id.equals(selectedCellID)){
            sessionController.clearSelectedCommit();
        }else{
            sessionController.selectCommit(id);
        }
        selectCommitInGraph(id);
    }

    /**
     * Handles mouse clicks that didn't happen on a cell. Deselects any
     * selected commit
     */
    public static void handleMouseClicked(){
        resetSelection();
    }

    /**
     * Takes in the cell that was moused over, and highlights it using highlightCommit
     * @param cell the cell generated the mouseover event
     * @param isOverCell whether the mouse is entering or exiting the cell
     */
    public static void handleMouseover(Cell cell, boolean isOverCell){
        highlightCommitInGraph(cell.getCellId(), isOverCell);
    }

    /**
     * If the commit with the given id is not selected, select it and deselect the previously
     * selected commit if necessary. If the current id is already selected, deselect it.
     * Loops through all tracked CommitTreeModels and updates their corresponding views.
     * @param commitID the id of the commit to select/deselect
     */
    private static void selectCommitInGraph(String commitID){
        boolean isDeselecting = commitID.equals(selectedCellID);

        for(CommitTreeModel model : allCommitTreeModels){
            if(model.treeGraph == null) continue;
            TreeGraphModel m = model.treeGraph.treeGraphModel;

            if(selectedCellID == null){
                selectCommitInGraph(commitID, m, true);
            }else{
                selectCommitInGraph(selectedCellID, m, false);
                if(!isDeselecting){
                    selectCommitInGraph(commitID, m, true);
                }
            }
        }
        if(isDeselecting){
            selectedCellID = null;
        }else{
            selectedCellID = commitID;
        }
        Edge.allVisible.set(selectedCellID == null);
    }

    /**
     * Highlight the commit with the given id in every tracked CommitTreeModel and corresponding
     * view. If the given id is selected, do nothing.
     * @param commitID the id of the commit to select
     * @param isOverCell whether to highlight or un-highlight the corresponding cells
     */
    private static void highlightCommitInGraph(String commitID, boolean isOverCell){
        for(CommitTreeModel model : allCommitTreeModels){
            if(model.treeGraph == null) continue;
            TreeGraphModel m = model.treeGraph.treeGraphModel;

            if(!isSelected(commitID)){
                Highlighter.highlightCell(commitID, selectedCellID, m, isOverCell);
                Highlighter.updateCellEdges(commitID, selectedCellID, m, isOverCell);
            }
        }
    }

    /**
     * Helper method that uses the Highlighter class to appropriately color the selected
     * commit and its edges
     * @param commitID the commit to select
     * @param model the model wherein the corresponding cell should be highlighted
     * @param enable whether to select or deselect the cell
     */
    private static void selectCommitInGraph(String commitID, TreeGraphModel model, boolean enable){
        Highlighter.highlightSelectedCell(commitID, model, enable);
        if(enable){
            Highlighter.updateCellEdges(commitID, commitID, model, true);
        }else{
            Highlighter.updateCellEdges(commitID, null, model, false);
        }
    }

    /**
     * Deselects the currently selected commit, if there is one
     */
    public static void resetSelection(){
        if(selectedCellID != null){
            selectCommitInGraph(selectedCellID);
        }
        sessionController.clearSelectedCommit();
    }

    /**
     * Checks to see if the given id is currently selected
     * @param cellID the id to check
     * @return true if it is selected, false otherwise
     */
    private static boolean isSelected(String cellID){
        return selectedCellID != null && selectedCellID.equals(cellID);
    }

    /**
     * Initializes the view corresponding to the given CommitTreeModel. Updates
     * all tracked CommitTreeModels with branch heads and missing commits,
     * but does not update their view
     * @param commitTreeModel the model whose view should be updated
     */
    public static void init(CommitTreeModel commitTreeModel){
        RepoHelper repo = commitTreeModel.sessionModel.getCurrentRepoHelper();

        List<String> commitIDs = repo.getAllCommitIDs();
        for(CommitTreeModel model : allCommitTreeModels){
            if(model.treeGraph != null){
                for(String id : commitIDs){
                    if(!model.containsID(id)){
                        model.addInvisibleCommit(id);
                    }
                }
                model.treeGraph.update();
            }
        }

        commitTreeModel.resetBranchHeads(false);
        List<BranchHelper> modelBranches = commitTreeModel.getBranches();
        if(modelBranches != null){
            for(BranchHelper branch : modelBranches){
                if(!commitTreeModel.sessionModel.getCurrentRepoHelper().isBranchTracked(branch)){
                    commitTreeModel.setCommitAsUntrackedBranch(branch.getHead().getId());
                }else{
                    commitTreeModel.setCommitAsTrackedBranch(branch.getHead().getId());
                }
            }
        }

        commitTreeModel.view.displayTreeGraph(commitTreeModel.treeGraph, commitTreeModel.sessionModel.getCurrentRepoHelper().getHead());
    }

    /**
     * Updates the views corresponding to all tracked CommitTreeModels after updating them
     * with branch heads and any missing commits
     * @param repo the repo from which the list of all commits is pulled
     */
    public static void update(RepoHelper repo) throws IOException{
        List<String> commitIDs = repo.getAllCommitIDs();
        for(CommitTreeModel model : allCommitTreeModels){
            if(model.treeGraph != null){
                for(String id : commitIDs){
                    if(!model.containsID(id)){
                        model.addInvisibleCommit(id);
                    }
                }

                model.resetBranchHeads(true);
                List<BranchHelper> modelBranches = model.getBranches();
                if(modelBranches == null) continue;
                for(BranchHelper branch : modelBranches){
                    if(!model.sessionModel.getCurrentRepoHelper().isBranchTracked(branch)){
                        model.setCommitAsUntrackedBranch(branch.getHeadID());
                    }else{
                        model.setCommitAsTrackedBranch(branch.getHeadID());
                    }
                }

                model.treeGraph.update();
                model.view.displayTreeGraph(model.treeGraph, null);
            }
        }
    }

    /**
     * Uses the Highlighter class to emphasize and scroll to the cell corresponding
     * to the given commit in every view corresponding to a tracked CommitTreeModel
     * @param commit the commit to focus
     */
    public static void focusCommitInGraph(CommitHelper commit){
        if(commit == null) return;
        for(CommitTreeModel model : allCommitTreeModels){
            if(model.treeGraph != null && model.treeGraph.treeGraphModel.containsID(commit.getId())){
                Cell c = model.treeGraph.treeGraphModel.cellMap.get(commit.getId());
                Highlighter.emphasizeCell(c);
            }
        }
    }

    /**
     * Uses the Highlighter class to emphasize and scroll to the cell corresponding
     * to the cell with the given ID in every view corresponding to a tracked CommitTreeModel
     * @param commitID the ID of the commit to focus
     */
    public static void focusCommitInGraph(String commitID){
        if(commitID == null) return;
        for(CommitTreeModel model : allCommitTreeModels){
            if(model.treeGraph != null && model.treeGraph.treeGraphModel.containsID(commitID)){
                Cell c = model.treeGraph.treeGraphModel.cellMap.get(commitID);
                Highlighter.emphasizeCell(c);
            }
        }
    }
}