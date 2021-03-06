package elegit;

import javafx.scene.control.Tooltip;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A subclass of RepoFile that contains a file that Git is ignoring.
 */
public class StagedRepoFile extends RepoFile {

    public StagedRepoFile(Path filePath, RepoHelper repo) {
        super(filePath, repo);
        diffButton.setText("STAGED");
        diffButton.setId("stagedDiffButton");
        diffButton.setTooltip(getToolTip("This file has a version stored in your git index\nand is ready to commit."));
    }

    public StagedRepoFile(String filePathString, RepoHelper repo) {
        this(Paths.get(filePathString), repo);
    }

    @Override public boolean canAdd() {
        return false;
    }

    @Override public boolean canRemove() {
        return true;
    }
}
