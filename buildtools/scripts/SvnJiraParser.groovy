import java.util.regex.Pattern;
import java.util.regex.Matcher;

import groovy.net.xmlrpc.XMLRPCServerProxy;
import groovy.util.CliBuilder;
import org.apache.commons.cli.Option;

/**
 * 1) Search through a svn xml log file and pull out the unique jira references
 * 2) Get the jiras from a given filter
 * 3) Create a list of unique jiras and write them to a file, with jira details 
 * TODO DZ: cleanup!  this file is a mess
 * TODO DZ: include svn fisheye links in the results
 * TODO DZ: pull in saleforce ids if the field is not null 
 */
public class SvnJiraParser 
{
    
    def JIRA_URI  = "http://www.mulesoft.org"
    def RPC_PATH  = "/jira/rpc/xmlrpc"
    def jiraUser = null
    def jiraPassword = null
    def outputFileName = null
    
    // jiras we are looking for start with these keys
    def jiraPrefixes = ["EE-", "MULE-"]
    def xmlFiles = []
    // will hold the list of unique jiras 
    def jiras = []
    
    def cliBuilder = new CliBuilder()
    def options = null
    
    def c = null
    def token = null
    def priorities = null
    def statuses = null
    def priorityMap = null
    def statusMap = null
    
    /**
     * The file to write
     */
    def outputFile = null
    /**
     * The jira filter to check for fixes in this release
     */
    def jiraFilter = null
    
    static void main(args)
    {
        SvnJiraParser sjp = new SvnJiraParser()
        //println("DEBUG: processCli : " + sjp.jiras.size())
        sjp.processCli(args)
        //println("DEBUG: initOutputFile : " + sjp.jiras.size())
        sjp.initOutputFile()
        //println("DEBUG: initJira : " + sjp.jiras.size())
        sjp.initJira()
        //println("DEBUG: doXmlFileWork : " + sjp.jiras.size())
        sjp.doXmlFileWork()
        println("DEBUG: issues found from xml files (non-unique): " + sjp.jiras.size())
        //println("DEBUG: getJirasFromFilter : " + sjp.jiras.size())
        sjp.getJirasFromFilter()
        //println("DEBUG: getJiraListDetails : " + sjp.jiras.size())
        sjp.getJiraListDetails(sjp.jiras)
        println("CSV File written to : " + sjp.outputFile)
        
        //TODO DZ: log out of jira
    }
    
    /**
     * Get the list of jiras from the xml files
     */
    void doXmlFileWork()
    {
        //iterate through the input files
        for(xmlFile in xmlFiles)
        {
            println("processing : " + xmlFile)
            // TODO DZ: make sure this is an xml file
            def entries = new XmlSlurper().parse(new File(xmlFile))
            println("Sorting through " + entries.children().size() + " changes")
            entries.children().each 
                    { logentry ->
                        for(int i = 0; i < jiraPrefixes.size(); i++) 
                        {
                            getMatching(jiraPrefixes[i], logentry.msg.text(), jiras)
                        }
                    }
        }                
    }
    
    /**
     * Parse the command-line options
     */
    void processCli(args)
    {
        cliBuilder.f(longOpt: "file", required: true, args: Option.UNLIMITED_VALUES, 
        valueSeparator : ',' as char, "svn xml log file (accepts multiple -f params)")
        cliBuilder.u(longOpt: "user", required: true, args: 1, "jira user name")
        cliBuilder.p(longOpt: "password", required: true, args: 1, "jira password")
        cliBuilder.o(longOpt: "output", required: true, args: 1, "File to write")
        cliBuilder.h(longOpt: "help", required: false, "Display usage message")
        cliBuilder.j(longOpt: "filter", required: true, args: 1, "jira filter")
        
        options = cliBuilder.parse(args)
        if (!args || !options)
        {
            println ""
            println "You must specify the required options"
            println ""
            System.exit(1)
        }
        
        if(options.h)
        {
            cliBuilder.usage()
            System.exit(1)
        }
        
        if (options.f)
        {
            xmlFiles = options.fs
        }    
        
        if (options.u)
        {
            jiraUser = options.u
        }
        
        if (options.p)
        {
            jiraPassword = options.p
        }
        
        if (options.o)
        {
            outputFileName = options.o
        }
        
        if (options.j)
        {
            jiraFilter = options.j
        }
    }
    
    /**
     * Find the string which contain jira keys and add them to the list
     */
    String getMatching(String prefix, String txt, List<String> jiras)
    {
        String results = "";
        String ResultString = null;
        Pattern regex = Pattern.compile("("+ prefix + "\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher regexMatcher = regex.matcher(txt);
        while (regexMatcher.find())
        {
            ResultString = regexMatcher.group();
            jiras.add(ResultString)
        }
    }
    
    /**
     * Initialize the connection to jira; save the auth token
     */
    void initJira()
    {
        c = new XMLRPCServerProxy(JIRA_URI + RPC_PATH)
        token = c.jira1.login(jiraUser, jiraPassword)
        
        priorities = c.jira1.getPriorities(token)
        statuses = c.jira1.getStatuses(token)
        
        priorityMap = getKeyedMap(priorities)
        statusMap = getKeyedMap(statuses)
    }
    
    /**
     * Create the output file, delete old one if it exists
     */
    void initOutputFile()
    {
        outputFile = new File(outputFileName);
        outputFile.delete(); //delete file so it only contains the latest results
        outputFile.append("Key, Summary, Last Updated, Status, Assignee, Priority, Automated Tests, Pass/Fail, Verified By, Notes\n");
    }
    
    /**
     * Given a list of jiras, fetch the details
     */
    List<String> getJiraListDetails(List<String> jiraList)
    {                      
        def count = 0
        def errorsString = ""
        
        def uniqueResults = jiras.unique().sort
                { a, b -> a.compareToIgnoreCase b }
        
        println("DEBUG: Final unique jira count : " + uniqueResults.size())
        
        for (jira in jiraList)
        {
            try
            {
                def issue = c.jira1.getIssue(token, jira.toString().toUpperCase())
                appendIssueToFile(issue)
            }
            catch (Exception e)
            {
                outputFile.append(jira + "\n")
                errorsString = errorsString + ("Can't get details in jira for : " + jira + "\n")
            }
            ++count
            if (count % 5 == 0)
            {
                print(".")
            }
        }    
        println("");
        print(errorsString)
    }
    
    /**
     * Assumes map contains properties 'id' and 'name', works for both jira priorities and statuses
     */
    Map<String, String> getKeyedMap(List srcList)
    {
        def destMap = [:]
        for(src in srcList)
        {
            destMap[src.id] = src.name
        }
        return destMap
    }
    
    /**
     * Add jiras from the specified filter to the list
     */
    void getJirasFromFilter()
    {        
        def errorsString = ""
        def count = 0
        def filterId
        try
        {
            
            def filters = c.jira1.getFavouriteFilters(token)
            for(filter in filters)
            {
                if(filter.name.equals(jiraFilter))
                {
                    filterId = filter.id 
                    break
                }
            }
            
            //println("DEBUG : filter id " + filterId)
            
            //TODO DZ: handle not finding the filter
            
            def issues = c.jira1.getIssuesFromFilter(token, filterId)
            
            println("DEBUG: unique issues found from jira filter : " + issues.size())
            
            for (issue in issues)
            {
                //appendIssueToFile(issue)
                jiras.add(issue.key)
            }
        }
        catch (Exception e)
        {
            println("Can't get jira filter : " + jiraFilter + "\n")
            println(e.toString())
        }       
    }
    
    /**
     * Write the jira to a csv file
     */
    void appendIssueToFile(issue)
    {
        // surround summary w/ double quotes since it may contain commas
        // escape double quotes in the summary field with a preceding double quote, i.e. text1 "FOO" text2 becomes text1 ""FOO"" text2
        def escapedSummary = "\"" + ((String)issue.summary).replaceAll("\"","\"\"") + "\"";
        outputFile.append(issue.key + ", " + escapedSummary + ", " + issue.updated + ", " + 
                statusMap[issue.status] + ", " + issue.assignee + ", " + priorityMap[issue.priority] + "\n")
    }
}
