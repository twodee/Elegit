package main.java.edugit;

import main.java.edugit.treefx.Cell;
import main.java.edugit.treefx.Edge;
import main.java.edugit.treefx.Highlighter;
import main.java.edugit.treefx.TreeGraphModel;

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

    /**
     * Takes in the cell that was clicked on, and selects it using selectCommit
     * @param cell the cell that was clicked
     */
    public static void handleMouseClicked(Cell cell){
        selectCommit(cell.getCellId());
    }

    /**
     * takes in the cell that was moused over, and highlights it using highlightCommit
     * @param cell the cell generated the mouseover event
     * @param isOverCell whether the mouse is entering or exiting the cell
     */
    public static void handleMouseover(Cell cell, boolean isOverCell){
        highlightCommit(cell.getCellId(), isOverCell);
    }

    /**
     * If the commit with the given id is not selected, select it and deselect the previously
     * selected commit if necessary. If the current id is already selected, deselect it.
     * Loops through all tracked CommitTreeModels and updates their corresponding views.
     * @param commitID the id of the commit to select/deselect
     */
    private static void selectCommit(String commitID){
        boolean isDeselecting = commitID.equals(selectedCellID);

        for(CommitTreeModel model : allCommitTreeModels){
            if(model.treeGraph == null) continue;
            TreeGraphModel m = model.treeGraph.treeGraphModel;

            if(selectedCellID == null){
                selectCommit(commitID, m, true);
            }else{
                selectCommit(selectedCellID, m, false);
                if(!isDeselecting){
                    selectCommit(commitID, m, true);
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
    private static void highlightCommit(String commitID, boolean isOverCell){
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
    private static void selectCommit(String commitID, TreeGraphModel model, boolean enable){
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
            selectCommit(selectedCellID);
        }
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
     * all tracked CommitTreeModels with missing commits, but does not update their view
     * @param commitTreeModel the model whose view should be updated
     */
    public static void init(CommitTreeModel commitTreeModel){
        RepoHelper repo = commitTreeModel.sessionModel.currentRepoHelper;
        List<String> commitIDs = repo.getAllCommitIDs();
        for(CommitTreeModel model : allCommitTreeModels){
            if(model.treeGraph != null){
                for(String id : commitIDs){
                    if(!model.containsID(id)){
                        model.addInvisibleCommit(id);
                    }
                }
                model.treeGraph.update();
                if(model.equals(commitTreeModel)){
                    model.view.displayTreeGraph(model.treeGraph);
                }
            }
        }
    }

    /**
     * Updates the views corresponding to all tracked CommitTreeModels after updating them
     * with any missing commits
     * @param repo the repo from which the list of all commits is pulled
     */
    public static void update(RepoHelper repo){
        List<String> commitIDs = repo.getAllCommitIDs();
        for(CommitTreeModel model : allCommitTreeModels){
            if(model.treeGraph != null){
                for(String id : commitIDs){
                    if(!model.containsID(id)){
                        model.addInvisibleCommit(id);
                    }
                }
                model.treeGraph.update();
                model.view.displayTreeGraph(model.treeGraph);
            }
        }
    }

    /**
     * Uses the Highlighter class to emphasize and scroll to the cell corresponding
     * to the given commit in every view corresponding to a tracked CommitTreeModel
     * @param commit the commit to focus
     */
    public static void focusCommit(CommitHelper commit){
        if(commit == null) return;
        for(CommitTreeModel model : allCommitTreeModels){
            if(model.treeGraph != null && model.treeGraph.treeGraphModel.containsID(commit.getId())){
                Cell c = model.treeGraph.treeGraphModel.cellMap.get(commit.getId());
                Highlighter.emphasizeCell(c);
            }
        }
    }
}