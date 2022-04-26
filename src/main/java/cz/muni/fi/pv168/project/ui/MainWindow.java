package cz.muni.fi.pv168.project.ui;

import cz.muni.fi.pv168.project.data.CategoryDao;
import cz.muni.fi.pv168.project.data.TaskDao;
import cz.muni.fi.pv168.project.model.Category;
import cz.muni.fi.pv168.project.model.Progress;
import cz.muni.fi.pv168.project.ui.control.ColoredButton;
import cz.muni.fi.pv168.project.ui.control.ColoredSeparator;
import cz.muni.fi.pv168.project.ui.control.LayoutSettings;
import cz.muni.fi.pv168.project.ui.dialog.CategoryDialog;
import cz.muni.fi.pv168.project.ui.dialog.EditDialog;
import cz.muni.fi.pv168.project.ui.dialog.TaskDialog;
import cz.muni.fi.pv168.project.ui.filter.TableFilter;
import cz.muni.fi.pv168.project.ui.i18n.I18N;
import cz.muni.fi.pv168.project.ui.model.CategoryModel;
import cz.muni.fi.pv168.project.ui.model.ComboBoxModelAdapter;
import cz.muni.fi.pv168.project.ui.model.TaskTableModel;
import cz.muni.fi.pv168.project.ui.renderer.ProgressRenderer;
import cz.muni.fi.pv168.project.ui.resources.AppColor;

import javax.sql.DataSource;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableRowSorter;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Class for creating the main window.
 */
public class MainWindow {

    private static final I18N I18N = new I18N(MainWindow.class);
    private final JFrame frame;
    private JPanel leftPanel;
    private JPanel rightPanel;
    private JPanel taskPanel;
    private JScrollPane tablePanel;

    private JButton deleteButton;
    private JButton editButton;

    private JComboBox<Category> categoryComboBox;
    private TableFilter tableFilter;
    private Progress lastProgress = null;

    private final DataSource dataSource;
    private final TaskDao taskDao;
    private final CategoryDao categoryDao;
    private final TaskTableModel taskTableModel;
    private JTable table;

    public MainWindow(DataSource dataSource) {
        this.dataSource = dataSource;
        this.categoryDao = new CategoryDao(dataSource);
        this.taskDao = new TaskDao(dataSource, categoryDao::findById);
        this.taskTableModel = new TaskTableModel(taskDao);

        frame = initializeFrame();
        initializeFrameContents();
        frame.pack();
    }

    private JFrame initializeFrame() {
        JFrame frame = new JFrame("ToDo List");

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(800, 600));
        frame.setMinimumSize(new Dimension(720, 380));
        return frame;
    }

    public void show() {
        frame.setVisible(true);
    }

    private void initializeFrameContents() {
        frame.setLayout(new GridBagLayout());

        createTablePanel();
        frame.add(tablePanel, new LayoutSettings(3, 0, 2, true,
                new Insets(5, 10, 10, 10)));

        createTaskPanel();
        frame.add(taskPanel, new LayoutSettings(2, 0, 2));
        frame.add(new ColoredSeparator(JSeparator.HORIZONTAL), new LayoutSettings(1, 0, 2));

        createRightPanel();
        frame.add(rightPanel, new LayoutSettings(0, 1, 1, false,
                new Insets(10, 10, 5, 10)));

        createLeftPanel();
        frame.add(leftPanel, new LayoutSettings(0, 0, 1, false,
                new Insets(10, 10, 5, 10)));
    }

    private void createTablePanel() {
        tablePanel = new JScrollPane(createTaskTable(taskTableModel),
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    private void createTaskPanel() {
        taskPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        addProgressButton(I18N.getString("progresses"), null, AppColor.NEUTRAL);
        addProgressButton(I18N.getString("planned"), Progress.PLANNED, AppColor.PLANNED);
        addProgressButton(I18N.getString("in_progress"), Progress.IN_PROGRESS, AppColor.IN_PROGRESS);
        addProgressButton(I18N.getString("finished"), Progress.FINISHED, AppColor.FINISHED);

        taskPanel.add(new ColoredSeparator(JSeparator.VERTICAL));

        deleteButton = new JButton(I18N.getString("delete"));
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(event -> deleteAndUpdate());
        taskPanel.add(deleteButton);

        editButton = new JButton(I18N.getString("edit"));
        editButton.setEnabled(false);
        editButton.addActionListener(event -> editAndUpdate(editButton));
        taskPanel.add(editButton);
    }

    private JTable createTaskTable(TaskTableModel taskTableModel) {
        table = new JTable(taskTableModel);

        var progressRenderer = new ProgressRenderer();
        table.setDefaultRenderer(Progress.class, progressRenderer);
        var progressComboBox = new JComboBox<>(Progress.values());
        progressComboBox.setRenderer(progressRenderer);
        table.setDefaultEditor(Progress.class, new DefaultCellEditor(progressComboBox));

        table.getSelectionModel().addListSelectionListener(this::rowSelectionChanged);
        table.setRowHeight(25);

        var rowSorter = new TableRowSorter<>(taskTableModel);
        tableFilter = new TableFilter(rowSorter);
        table.setRowSorter(rowSorter);

        return table;
    }

    private void createLeftPanel() {
        leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        var newTaskButton = new ColoredButton(I18N.getString("new_task"), AppColor.MAIN_BLUE.getColor());
        newTaskButton.addActionListener(event -> addAndUpdate(newTaskButton));
        leftPanel.add(newTaskButton);
    }

    private void createRightPanel() {
        rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        categoryComboBox = new JComboBox<>(new ComboBoxModelAdapter<>(new CategoryModel(categoryDao, true)));
        categoryComboBox.setSelectedIndex(0);
        categoryComboBox.setToolTipText(I18N.getString("category_filter"));
        categoryComboBox.addActionListener(event -> updateCategoryFilter());
        rightPanel.add(categoryComboBox);

        var newCategoryButton = new ColoredButton(I18N.getString("new_category"), AppColor.MAIN_BLUE.getColor());
        newCategoryButton.addActionListener(event -> addAndUpdateCategories(newCategoryButton));
        rightPanel.add(newCategoryButton);
    }

    private void rowSelectionChanged(ListSelectionEvent listSelectionEvent) {
        var selectionModel = (ListSelectionModel) listSelectionEvent.getSource();
        updateActions(selectionModel);
    }

    private void updateActions(ListSelectionModel selectionModel) {
        int selectedRowsCount = selectionModel.getSelectedItemsCount();
        editButton.setEnabled(selectedRowsCount == 1);
        deleteButton.setEnabled(selectedRowsCount >= 1);
    }

    private void addProgressButton(String label, Progress progress, AppColor color) {
        var button = new ColoredButton(label, color.getColor());
        button.addActionListener(event -> updateProgressFilter(progress));
        button.setToolTipText(I18N.getString("progress_filter"));
        taskPanel.add(button);
    }

    private void addAndUpdate(JButton newTaskButton) {
        frame.remove(tablePanel);
        new TaskDialog(dataSource, taskTableModel).show(newTaskButton);
        recreateTable();
    }

    private void editAndUpdate(JButton editButton) {
        frame.remove(tablePanel);
        var task = taskDao.findAll().get(table.getSelectedRow());
        new EditDialog(task, dataSource, taskTableModel).show(editButton);
        recreateTable();
    }

    private void deleteAndUpdate() {
        frame.remove(tablePanel);
        int confirmationOption = JOptionPane.showConfirmDialog(frame,
                I18N.getString("del_confirmation") + table.getSelectedRows().length,
                I18N.getString("del_confirmation_title"),
                JOptionPane.YES_NO_OPTION);
        if (confirmationOption == JOptionPane.YES_OPTION) {
            Arrays.stream(table.getSelectedRows())
                    .map(table::convertRowIndexToModel)
                    .boxed()
                    .sorted(Comparator.reverseOrder())
                    .forEach(taskTableModel::deleteRow);
        }
        recreateTable();
    }

    private void recreateTable() {
        tablePanel = new JScrollPane(createTaskTable(new TaskTableModel(taskDao)),
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        frame.add(tablePanel, new LayoutSettings(3, 0, 2, true,
                new Insets(5, 10, 10, 10)));
        SwingUtilities.updateComponentTreeUI(frame);
        updateCategoryFilter();
        if (lastProgress != null)
            updateProgressFilter(lastProgress);
        updateActions(table.getSelectionModel());
    }

    private void updateProgressFilter(Progress progress) {
        lastProgress = progress;
        List<Progress> progressList;
        if (progress == null) {
            progressList = Arrays.asList(Progress.values());
        } else {
            progressList = new ArrayList<>();
            progressList.add(progress);
        }
        tableFilter.filterProgress(progressList);
    }

    private void updateCategoryFilter() {
        var categoryList = new ArrayList<Category>();
        var category = categoryComboBox.getItemAt(categoryComboBox.getSelectedIndex());
        if (!category.isMain()) {
            categoryList.add(category);
        }
        tableFilter.filterCategory(categoryList);
    }

    private void addAndUpdateCategories(ColoredButton newCategoryButton) {
        new CategoryDialog(categoryDao).show(newCategoryButton);
        frame.remove(rightPanel);
        createRightPanel();
        frame.add(rightPanel, new LayoutSettings(0, 1, 1, false,
                new Insets(10, 10, 5, 10)));
        SwingUtilities.updateComponentTreeUI(frame);
    }
}
