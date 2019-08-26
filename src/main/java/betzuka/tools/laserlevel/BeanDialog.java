package betzuka.tools.laserlevel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.apache.commons.lang3.ClassUtils;

public class BeanDialog extends JDialog {
	
	public BeanDialog(JFrame parent, String title, Object bean) {
		super(parent, title);
		
		try {
			JPanel beanPanel = new JPanel();
			beanPanel.setLayout(new BorderLayout());
			beanPanel.add(createTable(bean), BorderLayout.CENTER);
			getContentPane().add(beanPanel);
			setDefaultCloseOperation(HIDE_ON_CLOSE);
			setPreferredSize(new Dimension(500,500));;
			pack();
		} catch (Exception e) {
			e.printStackTrace();
		}
			
	}

	private static JTable createTable (Object bean) throws Exception {
		
		BeanInfo info = Introspector.getBeanInfo(bean.getClass());
		
		
		List<PropertyDescriptor> props = new ArrayList<>();
		for (PropertyDescriptor prop : info.getPropertyDescriptors()) {
			if (prop.getWriteMethod()!=null && prop.getReadMethod()!=null) {
				props.add(prop);
			}
		}
		
		Collections.sort(props, new Comparator<PropertyDescriptor>() {
			@Override
			public int compare(PropertyDescriptor o1, PropertyDescriptor o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		
		
		TableModel model = new AbstractTableModel() {
			
						@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return columnIndex==1;
			}

			@Override
			public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
				if (columnIndex==1) {
					try {
						props.get(rowIndex).getWriteMethod().invoke(bean, aValue);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				if (columnIndex==0) {
					return props.get(rowIndex).getName();
				} else {
					try {
						return props.get(rowIndex).getReadMethod().invoke(bean);
					} catch (Exception e) {
						return new RuntimeException(e);
					}
				}
			}
			
			@Override
			public int getRowCount() {
				return props.size();
			}
			
			@Override
			public int getColumnCount() {
				return 2;
			}
		};
		
		JTable t = new JTable(model) {
			
			private Class editClass;
			
			@Override
            public TableCellRenderer getCellRenderer(int row, int column) {
				editClass = null;
                if (column == 1) {
                    return getDefaultRenderer(ClassUtils.primitiveToWrapper(props.get(row).getPropertyType()));
                } else {
                    return super.getCellRenderer(row, column);
                }
            }
			@Override
			public TableCellEditor getCellEditor(int row, int column) {
				editClass = null;
				if (column==0) {
					return super.getCellEditor(row, column);
				} 
				editClass = ClassUtils.primitiveToWrapper(props.get(row).getPropertyType());
				return getDefaultEditor(editClass);
			}
			@Override
            public Class getColumnClass(int column) {
                return editClass != null ? editClass : super.getColumnClass(column);
            }
			
		};
		
		t.setShowGrid(true);
		return t;
		
	}
	
	
	
}
