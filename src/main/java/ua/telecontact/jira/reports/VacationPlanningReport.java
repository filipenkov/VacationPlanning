package ua.telecontact.jira.reports;

import com.atlassian.jira.JiraApplicationContext;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.plugin.report.impl.AbstractReport;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.ParameterUtils;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.query.Query;
import java.time.Month;
import java.time.Year;
import java.time.LocalDateTime;
import java.util.*;
import java.sql.Timestamp;
import javax.servlet.http.HttpServletResponse;
import webwork.action.ActionContext;

@Scanned
public class VacationPlanningReport extends AbstractReport {
    private final JiraAuthenticationContext authContext;
    private final SearchService searchService;
    private final ProjectManager projectManager;
    private final DateTimeFormatter formatter;
    private final CustomFieldManager customFieldManager;
    private Date startDate;
    private Date endDate;

    public VacationPlanningReport(@ComponentImport JiraAuthenticationContext authContext,
                                  @ComponentImport SearchService searchService, @ComponentImport ProjectManager projectManager, @ComponentImport DateTimeFormatterFactory dateTimeFormatterFactory,
                                  @ComponentImport CustomFieldManager customFieldManager) {
        super();
        this.authContext = authContext;
        this.searchService = searchService;
        this.projectManager = projectManager;
        this.customFieldManager = customFieldManager;
        this.formatter = dateTimeFormatterFactory.formatter().withStyle(DateTimeStyle.DATE).forLoggedInUser();
    }

    public boolean isExcelViewSupported() {
        return true;
    }

    public String generateReportExcel(ProjectActionSupport action, Map reqParams) throws Exception {
        String startDateParam = (String)reqParams.get("periodFrom");
        String endDateParam = (String)reqParams.get("periodTo");
        StringBuffer contentDispositionValue = new StringBuffer(50);
        contentDispositionValue.append("attachment;filename=\"");
        contentDispositionValue.append("VacationPlanning_Report: ");
        contentDispositionValue.append(startDateParam + "-");
        contentDispositionValue.append(endDateParam);
        contentDispositionValue.append(".xls\";");
        HttpServletResponse response = ActionContext.getResponse();
        response.addHeader("content-disposition", contentDispositionValue.toString());
        return this.descriptor.getHtml("excel", this.getVelocityParams(action, reqParams));
    }

    public String generateReportHtml(ProjectActionSupport action, Map reqParams) throws Exception {
        final Map<String, Object> velocityParams = getVelocityParams(action, reqParams);
        return descriptor.getHtml("view", velocityParams);
    }
    private Map<String, Object> getVelocityParams(ProjectActionSupport action, Map reqParams) throws SearchException {
        String periodFrom = (String) reqParams.get("periodFrom");
        final Map<String, Object> velocityParams = new HashMap<String, Object>();
            velocityParams.put("report", this);
            velocityParams.put("action", action);
            velocityParams.put("issues", getIssuesFromProject());
            velocityParams.put("customFieldManager",customFieldManager);
            velocityParams.put("resultIssues",resultIssues(reqParams,getIssuesFromProject()));
            velocityParams.put("daysInPeriod",daysInPeriod(reqParams));
        return velocityParams;
    }
    List<Issue> getIssuesFromProject() throws SearchException {
        JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();
        builder.where().project().eq("OK");
        Query query = builder.buildQuery();
        SearchResults results = this.searchService.search(this.authContext.getLoggedInUser(), query, PagerFilter.getUnlimitedFilter());
        return results.getIssues();
    }
    public HashMap<Issue,ArrayList<Integer>> resultIssues(Map reqParams, List<Issue> issues){
        String periodFrom = (String) reqParams.get("periodFrom");
        String periodTo = (String) reqParams.get("periodTo");
        Timestamp cfPeriodFrom = null;
        Timestamp cfPeriodTo = null;
        HashMap<Issue,ArrayList<Integer>> result = new HashMap<Issue,ArrayList<Integer>>();
        if(issues.size()>0){
            for(Issue issue : issues){
                ArrayList<Integer> bool = new ArrayList<Integer>();
                cfPeriodFrom = (Timestamp) issue.getCustomFieldValue(customFieldManager.getCustomFieldObject(10100L));
                cfPeriodTo = (Timestamp) issue.getCustomFieldValue(customFieldManager.getCustomFieldObject(10101L));
                startDate = formatter.parse(periodFrom);
                endDate = formatter.parse(periodTo);
                for(int x = startDate.getDate();x<=endDate.getDate();x++){
                    startDate.setDate(x);
                    if((startDate.after(cfPeriodFrom) && cfPeriodTo.after(startDate)) || startDate.equals(cfPeriodTo) || startDate.equals(cfPeriodFrom)){
                        bool.add(1);
                    }else{
                        bool.add(0);
                    }
                }
                result.put(issue,bool);
            }
        }
        return result;
    }
    public ArrayList<Integer> daysInPeriod(Map reqParams){
        ArrayList<Integer> result = new ArrayList<Integer>();
        String periodFrom = (String) reqParams.get("periodFrom");
        String periodTo = (String) reqParams.get("periodTo");
        startDate = formatter.parse(periodFrom);
        endDate = formatter.parse(periodTo);
        for(int x = startDate.getDate();x<=endDate.getDate();x++){
            result.add(x);
        }
        return result;
    }
    @Override
    public void validate(ProjectActionSupport action, Map reqParams){
        String periodFrom = (String) reqParams.get("periodFrom");
        String periodTo = (String) reqParams.get("periodTo");
        try {
            startDate = formatter.parse(periodFrom);
        }catch (IllegalArgumentException var1){
            action.addError("periodFrom",action.getText("vacation-planning-report.periodFrom.validate"));
        }
        try{
            endDate = formatter.parse(periodTo);
        }catch (IllegalArgumentException var2){
            action.addError("periodTo",action.getText("vacation-planning-report.periodTo.validate"));
        }
        super.validate(action, reqParams);
    }
}