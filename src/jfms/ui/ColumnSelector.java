package jfms.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

public class ColumnSelector {
	private final List<TreeTableColumn<Message,?>> availableColumns;
	private List<TreeTableColumn<Message,?>> columns = Collections.emptyList();
	private List<TreeTableColumn<Message,?>> sortOrder = Collections.emptyList();

	public ColumnSelector(List<TreeTableColumn<Message,?>> columns) {
		availableColumns = new ArrayList<>(columns);
	}

	public void setParameters(TreeTableView<Message> table) {
		columns = new ArrayList<>();
		sortOrder = new ArrayList<>();

		for (TreeTableColumn<Message,?> s : table.getColumns()) {
			for (TreeTableColumn<Message,?> c : availableColumns) {
				if (s.getText().equals(c.getText())) {
					c.setVisible(s.isVisible());
					columns.add(c);
					break;
				}
			}
		}

		for (TreeTableColumn<Message,?> s : table.getSortOrder()) {
			for (TreeTableColumn<Message,?> c : columns) {
				if (s.getText().equals(c.getText())) {
					c.setSortType(s.getSortType());
					sortOrder.add(c);
					break;
				}
			}
		}
	}

	public void setParameters(String columnSpec, String sortSpec) {
		columns = new ArrayList<>();
		sortOrder = new ArrayList<>();
		List<TreeTableColumn<Message,?>> remainingColumns =
				new ArrayList<>(availableColumns);

		for (String s : columnSpec.split(",")) {
			for (TreeTableColumn<Message,?> c : remainingColumns) {
				if (s.equals(c.getText())) {
					c.setVisible(true);
					columns.add(c);
					remainingColumns.remove(c);
					break;
				}
			}
		}

		for (TreeTableColumn<Message,?> c : remainingColumns) {
			c.setVisible(false);
			columns.add(c);
		}

		for (String s : sortSpec.split(",")) {
			if (s.length() < 2) {
				continue;
			}

			TreeTableColumn.SortType sortType;
			if (s.charAt(0) == '+') {
				sortType = TreeTableColumn.SortType.ASCENDING;
			} else {
				sortType = TreeTableColumn.SortType.DESCENDING;
			}
			String name = s.substring(1);

			for (TreeTableColumn<Message,?> c : columns) {
				if (name.equals(c.getText())) {
					c.setSortType(sortType);
					sortOrder.add(c);
					break;
				}
			}
		}
	}

	public void apply(TreeTableView<Message> table) {
		table.getColumns().setAll(columns);
		table.getSortOrder().setAll(sortOrder);
		table.setTreeColumn(null);

		int pos = 0;
		boolean setTreeColumn = false;
		for (TreeTableColumn<Message,?> c : table.getColumns()) {
			// we don't want the starred column to contain the disclosure node
			// (the arrow for expanding).
			// If the starred column is the first column, explicitly set
			// tree column to second column
			if (setTreeColumn) {
				table.setTreeColumn(c);
				setTreeColumn = false;
			}
			if (pos++ == 0 && "Starred".equals(c.getText())) {
				double width = Icons.getInstance().getStarredIcon().getWidth();
				c.setPrefWidth(width+8);
				setTreeColumn = true;
			}
		}
	}

	public String getColumnSpec() {
		if (columns == null) {
			return "";
		}

		StringBuilder cols = new StringBuilder();
		for (TreeTableColumn<Message,?> c : columns) {
			if (c.isVisible()) {
				if (cols.length() > 0) {
					cols.append(',');
				}

				cols.append(c.getText());
			}
		}

		return cols.toString();
	}

	public String getSortSpec() {
		if (sortOrder == null) {
			return "";
		}

		StringBuilder sort = new StringBuilder();
		for (TreeTableColumn<Message,?> c : sortOrder) {
			if (sort.length() > 0) {
				sort.append(',');
			}

			if (c.getSortType() == TreeTableColumn.SortType.ASCENDING) {
				sort.append('+');
			} else {
				sort.append('-');
			}
			sort.append(c.getText());
		}

		return sort.toString();
	}

	public TreeTableColumn<Message,?> getColumn(String name) {
		TreeTableColumn<Message,?> col = null;
		for (TreeTableColumn<Message,?> c : availableColumns) {
			if (name.equals(c.getText())) {
				col = c;
				break;
			}
		}

		return col;
	}
}
