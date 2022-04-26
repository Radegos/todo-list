package cz.muni.fi.pv168.project.ui.filter;

import cz.muni.fi.pv168.project.model.Category;
import cz.muni.fi.pv168.project.model.Progress;
import cz.muni.fi.pv168.project.model.Task;
import cz.muni.fi.pv168.project.ui.model.TaskTableModel;

import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for representing a table filter.
 *
 */
public final class TableFilter {

    private final TableRowSorter<TaskTableModel> rowSorter;
    private List<Progress> progressList = new ArrayList<>();
    private List<Category> categoryList = new ArrayList<>();

    public TableFilter(TableRowSorter<TaskTableModel> rowSorter) {
        this.rowSorter = rowSorter;
    }

    public void filterProgress(List<Progress> progresses) {
        progressList = progresses;
        rowSorter.setRowFilter(new TableRowFilter(progressList, categoryList));
    }

    public void filterCategory(List<Category> categories) {
        categoryList = categories;
        rowSorter.setRowFilter(new TableRowFilter(progressList, categoryList));
    }

    private static class TableRowFilter extends RowFilter<TaskTableModel, Integer> {

        private final List<Progress> progresses;
        private final List<Category> categories;

        private TableRowFilter(List<Progress> progresses, List<Category> categories) {
            this.progresses = progresses;
            this.categories = categories;
        }

        @Override
        public boolean include(Entry<? extends TaskTableModel, ? extends Integer> entry) {
            TaskTableModel tableModel = entry.getModel();
            Task task = tableModel.getEntity(entry.getIdentifier());
            return (progresses.isEmpty() || progresses.contains(task.getProgress())) &&
                    (categories.isEmpty() || categories.contains(task.getCategory()));
        }
    }
}
