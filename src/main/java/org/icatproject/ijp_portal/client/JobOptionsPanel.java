package org.icatproject.ijp_portal.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.icatproject.ijp_portal.client.parser.ExpressionEvaluator;
import org.icatproject.ijp_portal.client.parser.ParserException;
import org.icatproject.ijp_portal.client.service.DataService;
import org.icatproject.ijp_portal.client.service.DataServiceAsync;
import org.icatproject.ijp_portal.shared.DatasetOverview;
import org.icatproject.ijp_portal.shared.PortalUtils;
import org.icatproject.ijp_portal.shared.PortalUtils.MultiJobTypes;
import org.icatproject.ijp_portal.shared.xmlmodel.JobOption;
import org.icatproject.ijp_portal.shared.xmlmodel.JobType;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DoubleBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.LongBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ValueBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class JobOptionsPanel extends VerticalPanel {

	private DataServiceAsync dataService = GWT.create(DataService.class);

	Portal portal;
	DialogBox dialogBox;

	CheckBox confirmMultipleCheckBox = new CheckBox();
	ListBox multipleDatasetsOptionListBox = new ListBox();
	
	Button closeButton = new Button("Close");
	Button submitButton = new Button("Submit");

	Map<JobOption, Widget> jobOptionToFormWidgetMap;

	
	public JobOptionsPanel(final Portal portal, final DialogBox dialogBox) {
		this.portal = portal;
		this.dialogBox = dialogBox;
		
		multipleDatasetsOptionListBox.addItem("Please select...", "");
		multipleDatasetsOptionListBox.addItem("Submit multiple datasets to one job", MultiJobTypes.MULTIPLE_DATASETS_ONE_JOB.name());
		multipleDatasetsOptionListBox.addItem("Run multiple jobs - one dataset per job", MultiJobTypes.ONE_DATASET_PER_JOB.name());
		
		closeButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				dialogBox.hide();
				portal.datasetsPanel.datasetActionListBox.setSelectedIndex(0);
			}
		});
	
		submitButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				List<String> optionsList = new ArrayList<String>();
				List<String> formErrors = new ArrayList<String>();
				// most jobs are of this type - change it further down in specific cases
				PortalUtils.MultiJobTypes multiJobType = MultiJobTypes.ONE_DATASET_PER_JOB;
				
				String jobName = portal.datasetsPanel.datasetActionListBox.getValue(portal.datasetsPanel.datasetActionListBox.getSelectedIndex());
				JobType jobType = portal.datasetsPanel.jobTypeMappings.getJobTypesMap().get(jobName);
				int numSelectedDatasets = portal.datasetsPanel.selectionModel.getSelectedSet().size();
				if ( numSelectedDatasets > 1 ) {
					if ( jobType.getType().equalsIgnoreCase("interactive") ) {
						multiJobType = MultiJobTypes.MULTIPLE_DATASETS_ONE_JOB;
					} else if ( jobType.getType().equalsIgnoreCase("batch") ) {
						if ( jobType.getMultiple() == false ) {
							if ( confirmMultipleCheckBox.getValue() == false ) {
								formErrors.add("Please tick the box at the top of the form to confirm your intention to run multiple jobs");
							}
						} else {
							if ( multipleDatasetsOptionListBox.getSelectedIndex() == 0 ) {
								formErrors.add("Please select the type of job you intend to run at the top of the form");
							} else {
								multiJobType = MultiJobTypes.valueOf(multipleDatasetsOptionListBox.getValue(multipleDatasetsOptionListBox.getSelectedIndex()));
							}
						}
					}
				}
				
				for ( JobOption jobOption : jobOptionToFormWidgetMap.keySet() ) {
					// firstly check that this is a program parameter we can set
					// the standard "View" option for MSMM Viewer appears on the form but no parameters
					// get added to the command line if it is selected, for example
					if ( jobOption.getProgramParameter() != null && !jobOption.getProgramParameter().equals("") ) {
						Widget formWidget = jobOptionToFormWidgetMap.get(jobOption);
						if (formWidget.getClass() == RadioButton.class || formWidget.getClass() == CheckBox.class) {
							// we can do this because RadioButton extends CheckBox
							if ( ((CheckBox)formWidget).getValue() == true ) {
								optionsList.add( jobOption.getProgramParameter() );
							}
						} else if (formWidget.getClass() == ListBox.class) {
							String selectedListBoxValue = ((ListBox)formWidget).getValue(((ListBox)formWidget).getSelectedIndex());
							if ( selectedListBoxValue != null && !selectedListBoxValue.equals("") ) {
								optionsList.add( jobOption.getProgramParameter() + " " + selectedListBoxValue );
							}
						} else if (formWidget.getClass() == TextBox.class) {
							String textBoxValue = ((TextBox)formWidget).getValue();
							if ( !textBoxValue.equals("") ) {
								optionsList.add( jobOption.getProgramParameter() + " " + textBoxValue );
							}
						} else if (formWidget.getClass() == LongBox.class || formWidget.getClass() == DoubleBox.class) {
							Number numericBoxValueNumber = (Number) ((ValueBox)formWidget).getValue();
							if ( numericBoxValueNumber != null ) {
								double numericBoxValueDouble = numericBoxValueNumber.doubleValue();
								String minValueString = jobOption.getMinValue();
								if ( minValueString != null && !minValueString.equals("") ) {
									// TODO - should these parse checks have already been done when reading the XML for example?
									try {
										double minValueDouble = Double.parseDouble(minValueString);
										if ( numericBoxValueDouble < minValueDouble ) {
											formErrors.add("Submitted value '" + numericBoxValueDouble + "' for job option '" + jobOption.getName() + "' must be greater than '" + minValueString + "'");
										}
									} catch (NumberFormatException e) {
										formErrors.add("minValue '" + minValueString + "' of job option '" + jobOption.getName() + "' is not a valid number");
									}
								}
								String maxValueString = jobOption.getMaxValue();
								if ( maxValueString != null && !maxValueString.equals("") ) {
									// TODO - should these parse checks have already been done when reading the XML for example?
									try {
										double maxValueDouble = Double.parseDouble(maxValueString);
										if ( numericBoxValueDouble > maxValueDouble ) {
											formErrors.add("Submitted value '" + numericBoxValueDouble + "' for job option '" + jobOption.getName() + "' must be less than '" + maxValueString + "'");
										}
									} catch (NumberFormatException e) {
										formErrors.add("maxValue '" + maxValueString + "' of job option '" + jobOption.getName() + "' is not a valid number");
									}
								}
								optionsList.add( jobOption.getProgramParameter() + " " + numericBoxValueDouble );
							}
						}
					}
				}
				if ( formErrors.size() > 0 ) {
					// show all the errors in one alert message
					StringBuilder formErrorsMessage = new StringBuilder("Job cannot be submitted due to the following errors:\n");
					for ( String formError : formErrors ) {
						formErrorsMessage.append(" - ");
						formErrorsMessage.append(formError);
						formErrorsMessage.append("\n");
					}
					Window.alert(formErrorsMessage.toString());
				} else {
					// just display the job name, options string and dataset ids in an alert for now
					// TODO - send this off to the server to get a job executed
					List<String> datasetIdsList = new ArrayList<String>();
					for ( DatasetOverview selectedDataset : portal.datasetsPanel.selectionModel.getSelectedSet() ) {
						String datasetId = Long.toString(selectedDataset.getDatasetId());
						datasetIdsList.add(datasetId);
					}
					String alertMessage = "";
					alertMessage += "jobName = '" + jobName + "'\n";
					alertMessage += "optionsString = '" + PortalUtils.createStringFromList(optionsList, " ") + "'\n";
					alertMessage += "datasetIdsString = '" + PortalUtils.createStringFromList(datasetIdsList, ",") + "'\n";
					alertMessage += "multiJobType = '" + multiJobType.name() + "'\n";
					Window.alert(alertMessage);
				}
			}
		});
	}

	void populateAndShowForm() {
		// clear the current form
		clear();
		confirmMultipleCheckBox.setValue(false);
		multipleDatasetsOptionListBox.setSelectedIndex(0);

		// get a list of selected dataset ids
		List<Long> selectedDatasetIds = new ArrayList<Long>();
		for ( DatasetOverview selectedDataset : portal.datasetsPanel.selectionModel.getSelectedSet() ) {
			selectedDatasetIds.add(selectedDataset.getDatasetId());
		}
		String datasetType = portal.datasetsPanel.datasetTypeListBox.getValue(portal.datasetsPanel.datasetTypeListBox.getSelectedIndex());

		dataService.getJobDatasetParametersForDatasets(portal.getSessionId(), datasetType, selectedDatasetIds, new AsyncCallback<Map<Long, Map<String, Object>>>() {  
			@Override
			public void onFailure(Throwable caught) {
				Window.alert("Server error: " + caught.getMessage());
			}
	
			@Override
			public void onSuccess(Map<Long, Map<String, Object>> jobDatasetParametersForDatasets) {
				String jobName = portal.datasetsPanel.datasetActionListBox.getValue(portal.datasetsPanel.datasetActionListBox.getSelectedIndex());
				jobOptionToFormWidgetMap = new LinkedHashMap<JobOption, Widget>();
				Map<String, HorizontalPanel> nameToPanelMap = new LinkedHashMap<String, HorizontalPanel>();
				JobType jobType = portal.datasetsPanel.jobTypeMappings.getJobTypesMap().get(jobName);

				for (JobOption jobOption : jobType.getJobOptions()) {
					// firstly work out if this option should be available for the selected dataset
					boolean makeOptionAvailable = false;
					String condition = jobOption.getCondition();
					if ( condition == null || condition.equals("") ) {
						// options with an empty condition are offered for all datasets of this type
						makeOptionAvailable = true;
					} else {
						// first check that the call to the server was successful
						if ( jobDatasetParametersForDatasets != null ) {
							// options with a non-empty condition are only offered if the condition is met
							// and the condition has to be met for all of the selected datasets
//							for ( DatasetOverview selectedDataset : portal.datasetsPanelNew.selectionModel.getSelectedSet() ) {
								for ( Long selectedDatasetId : jobDatasetParametersForDatasets.keySet() ) {
								boolean conditionMatch = false;
								try {
									conditionMatch = ExpressionEvaluator.isTrue(condition, jobDatasetParametersForDatasets.get(selectedDatasetId));
								} catch (ParserException e) {
									Window.alert("ParserException: " + e.getMessage());
								}
								if ( conditionMatch ) {
									makeOptionAvailable = true;
								} else {
									makeOptionAvailable = false;
//									Window.alert("Condition '" + condition
//											+ "' for Job Option '" + jobOption.getName()
//											+ "' not met for Dataset "
//											+ selectedDataset.getDatasetId());
									// no need to check any more datasets so stop looping now
									break;
								}
							}
						}
					}
					
					if (makeOptionAvailable) {
						HorizontalPanel hp = new HorizontalPanel();
						boolean optionLabelRequired = true;
						String optionName = jobOption.getName();
						Widget formWidget = null;
						if ( jobOption.getType().equals("boolean") ) {
							if ( jobOption.getGroupName() != null && !jobOption.getGroupName().equals("") ) {
								RadioButton radioButton = new RadioButton(jobOption.getGroupName(), jobOption.getName());
								HorizontalPanel existingPanel = nameToPanelMap.get(jobOption.getGroupName());
								if ( existingPanel != null ) {
									// set this as the panel we are going to add to - it already has a label
									hp = existingPanel;
									// the panel we are adding to will already have a label 
									// so set a flag so that another one is not added
									optionLabelRequired = false;
								} else {
									// this is the first of a group of buttons so check this one
									radioButton.setValue(true);
								}
								// for radio buttons use the group name for the name of the option
								optionName = jobOption.getGroupName();
								formWidget = radioButton;
							} else {
								formWidget = new CheckBox();
							}
						} else if (jobOption.getType().equals("enumeration") ) {
							ListBox listBox = new ListBox();
							for (String value : jobOption.getValues() ) {
								listBox.addItem(value);
							}
							formWidget = listBox;
						} else if (jobOption.getType().equals("string") ) {
							formWidget = new TextBox();
						} else if (jobOption.getType().equals("integer") ) {
							formWidget = new LongBox();
						} else if (jobOption.getType().equals("float") ) {
							formWidget = new DoubleBox();
						}

						// add an option label and a tool tip if needed
						if ( optionLabelRequired ) {
							HTML optionNameHTML = new HTML("<b>" + optionName + "</b>&nbsp;"); 
							hp.add(optionNameHTML);
							if ( jobOption.getTip() != null && !jobOption.getTip().equals("") ) {
								optionNameHTML.setTitle( jobOption.getTip() );
							}
						}
						
						// add the form element itself
						hp.add(formWidget);
						nameToPanelMap.put(optionName, hp);
						jobOptionToFormWidgetMap.put(jobOption, formWidget);
						
						// add any extra info - default, min, max values etc
						if ( jobOption.getDefaultValue() != null && !jobOption.getDefaultValue().equals("") ) {
							hp.add(new HTML("&nbsp;<i>(default=" + jobOption.getDefaultValue() + ")</i>"));
						}
						if ( jobOption.getMinValue() != null && !jobOption.getMinValue().equals("") ) {
							hp.add(new HTML("&nbsp;<i>(min=" + jobOption.getMinValue() + ")</i>"));
						}
						if ( jobOption.getMaxValue() != null && !jobOption.getMaxValue().equals("") ) {
							hp.add(new HTML("&nbsp;<i>(max=" + jobOption.getMaxValue() + ")</i>"));
						}
					}
				}
				
				int numSelectedDatasets = portal.datasetsPanel.selectionModel.getSelectedSet().size();
				if ( jobType.getType().equalsIgnoreCase("batch") && numSelectedDatasets > 1  ) {
					HorizontalPanel warningPanel = new HorizontalPanel();	
					if ( jobType.getMultiple() == false ) {
						// warn user that they are submitting multiple jobs
						// get them to tick a checkbox to confirm
						warningPanel.add(new HTML("<font color='red'><b>" +
								"Please tick the box to confirm that you are intending to run <br> " +
								numSelectedDatasets + " '" + jobType.getName() +
								"' jobs - one job per selected dataset  &nbsp;</b></font>"));
						warningPanel.add(confirmMultipleCheckBox);
					} else {
						// warn the user and offer the following two options 
						// multiple datasets to one job or separate job for each dataset
						warningPanel.add(new HTML("<font color='red'><b>" + 
								"Please select the type of job you intend to run &nbsp;</b></font>"));
						warningPanel.add(multipleDatasetsOptionListBox);
					}
					add(warningPanel);
					add(new HTML("<hr></hr>"));
				}
				
				for ( String name : nameToPanelMap.keySet() ) {
					HorizontalPanel hp = nameToPanelMap.get(name);
					add(hp);
					add(new HTML("<hr></hr>"));
				}
				
				HorizontalPanel footerPanel = new HorizontalPanel();
				footerPanel.setWidth("100%");
				footerPanel.add(submitButton);
				footerPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
				footerPanel.add(closeButton);
				add(footerPanel);
				
				portal.jobOptionsDialog.setText(jobName + " Options");
				portal.jobOptionsDialog.show();
			}
		});

	}
	
}
