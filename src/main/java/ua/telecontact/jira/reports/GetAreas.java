package ua.telecontact.jira.reports;

import com.atlassian.configurable.ValuesGenerator;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.context.IssueContext;
import com.atlassian.jira.issue.context.IssueContextImpl;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.project.Project;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Collection;

public class GetAreas implements ValuesGenerator<String>{
    public Map<String, String> getValues(Map userParams){
        Map<String, String> areasMap = new HashMap<String, String>();
        //areasMap.put(" "," ");
        CustomFieldManager customFieldmanager = ComponentAccessor.getCustomFieldManager();
        OptionsManager optionsManager = ComponentAccessor.getOptionsManager();
        Collection<CustomField> cf = customFieldmanager.getCustomFieldObjectsByName("Площадка");
        IssueContextImpl issueContext = new IssueContextImpl(10000L, "10000");
        FieldConfig fieldConfig = cf.iterator().next().getRelevantConfig(issueContext);
        Options options = optionsManager.getOptions(fieldConfig);
        for(Option option : options){
            areasMap.put(option.getOptionId().toString(),option.getValue());
        }
        return areasMap;
    }
}