package samplereporter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.testng.Assert;
import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.TestListenerAdapter;
import org.testng.internal.Utils;
import org.testng.reporters.util.StackTraceTools;
import org.testng.xml.XmlSuite;

import utils.PropertyUtils;

public class HTMLReporter extends TestListenerAdapter implements IReporter
{
    private static PrintWriter f_out;
    private static String      outputDir;

    private static String[]    MODULES;
    private static String[]    TEST_GROUPS;

    public void generateReport(List<XmlSuite> arg0, List<ISuite> suites, String outdir)
    {
        try
        {
            outputDir = PropertyUtils.getProperty("app.test.reports.directory");
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

        String modulesCommaSeparated = "";
        String testGroupsCommaSeparated = "";

        try
        {
            modulesCommaSeparated = PropertyUtils.getProperty("app.application.test.modules").replaceAll("\\s+", "");
            testGroupsCommaSeparated = PropertyUtils.getProperty("app.application.test.groups").replaceAll("\\s+", "");
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

        if (modulesCommaSeparated == null || modulesCommaSeparated.trim().length() == 0)
        {
            Assert.fail("ERROR - Modules are not found in properties file");
        } else
        {
            MODULES = new String[modulesCommaSeparated.length()];
            MODULES = modulesCommaSeparated.split(",");
        }

        if (testGroupsCommaSeparated == null || testGroupsCommaSeparated.trim().length() == 0)
        {
            Assert.fail("ERROR - Test Groups are not found in properties file");
        } else
        {
            TEST_GROUPS = new String[testGroupsCommaSeparated.length()];
            TEST_GROUPS = testGroupsCommaSeparated.split(",");
        }

        try
        {
            f_out = createWriter(outputDir);
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        startHtmlPage(f_out);

        try
        {
            generateAdditionalInfoReport(suites);
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        generateTestExecutionStatus(suites);
        endHtmlPage(f_out);

        f_out.flush();
        f_out.close();

    }

    private void generateTestExecutionStatus(List<ISuite> suites)
    {
        String testName = "";

        int totalPassedMethods = 0;
        int totalFailedMethods = 0;
        int totalSkippedMethods = 0;
        int totalSkippedConfigurationMethods = 0;
        int totalFailedConfigurationMethods = 0;
        int totalMethods = 0;

        int suite_totalPassedMethods = 0;
        int suite_totalFailedMethods = 0;
        int suite_totalSkippedMethods = 0;

        String suite_passPercentage = "";
        String suiteName = "";

        ITestContext overview = null;
        HashMap<String, String> dashboardReportMap = new HashMap<String, String>();

        for (ISuite suite : suites)
        {
            suiteName = suite.getName();

            Map<String, ISuiteResult> tests = suite.getResults();

            for (ISuiteResult r : tests.values())
            {
                overview = r.getTestContext();
                testName = overview.getName();

                totalPassedMethods = overview.getPassedTests().getAllMethods().size();
                totalFailedMethods = overview.getFailedTests().getAllMethods().size();
                totalSkippedMethods = overview.getSkippedTests().getAllMethods().size();

                totalMethods = overview.getAllTestMethods().length;

                NumberFormat nf = NumberFormat.getInstance();
                nf.setMaximumFractionDigits(2);
                nf.setGroupingUsed(true);

                String includedModule = "";
                String includedGroup = "";

                ITestNGMethod[] allTestMethods = overview.getAllTestMethods();
                for (ITestNGMethod testngMethod : allTestMethods)
                {
                    String[] modules = testngMethod.getGroups();
                    for (String module : modules)
                    {
                        for (String moduleName : MODULES)
                        {
                            if (module.equalsIgnoreCase(moduleName))
                            {
                                if (!(includedModule.contains(module)))
                                {
                                    includedModule = includedModule + " " + module;
                                }
                            }
                        }
                        for (String groupName : TEST_GROUPS)
                        {
                            if (module.equalsIgnoreCase(groupName))
                            {
                                if (!(includedGroup.contains(module)))
                                {
                                    includedGroup = includedGroup + " " + module;
                                }
                            }
                        }
                    }
                }

                String browser = overview.getCurrentXmlTest().getParameter("browser");
                String browser_version = overview.getCurrentXmlTest().getParameter("browser_version");
                String platform = overview.getCurrentXmlTest().getParameter("os");

                if (platform == null || platform.trim().length() == 0)
                {
                    platform = "N/A";
                }

                if (browser_version == null || browser_version.trim().length() == 0)
                {
                    browser_version = "N/A";
                }

                if (browser == null || browser.trim().length() == 0)
                {
                    browser = "N/A";
                }

                if (!(dashboardReportMap.containsKey(includedModule)))
                {
                    if (browser_version.equalsIgnoreCase("N/A"))
                    {
                        browser_version = "";
                    }
                    dashboardReportMap.put(includedModule, "os1~" + platform + "|browser1~" + browser + browser_version
                            + "|testcase_count_1~" + totalMethods + "|pass_count_1~" + totalPassedMethods
                            + "|fail_count_1~" + totalFailedMethods + "|skip_count_1~" + totalSkippedMethods
                            + "|skip_conf_count_1~" + totalSkippedConfigurationMethods + "|fail_conf_count_1~"
                            + totalFailedConfigurationMethods);

                } else
                {
                    for (String key : dashboardReportMap.keySet())
                    {

                        if (key.equalsIgnoreCase(includedModule))
                        {
                            if (browser_version.equalsIgnoreCase("N/A"))
                            {
                                browser_version = "";
                            }
                            String value = dashboardReportMap.get(key);
                            int index = StringUtils.countMatches(value, "#") + 1;

                            index += 1;

                            value = value + "#" + "os" + index + "~" + platform + "|browser" + index + "~" + browser
                                    + browser_version + "|testcase_count_" + index + "~" + totalMethods
                                    + "|pass_count_" + index + "~" + totalPassedMethods + "|fail_count_" + index + "~"
                                    + totalFailedMethods + "|skip_count_" + index + "~" + totalSkippedMethods
                                    + "|skip_conf_count_" + index + "~" + totalSkippedConfigurationMethods
                                    + "|fail_conf_count_" + index + "~" + totalFailedConfigurationMethods;
                            dashboardReportMap.put(key, value);
                        }
                    }
                }

                suite_totalPassedMethods += totalPassedMethods;
                suite_totalFailedMethods += totalFailedMethods;
                suite_totalSkippedMethods += totalSkippedMethods;

                try
                {
                    suite_passPercentage = nf
                            .format(((float) suite_totalPassedMethods / (float) (suite_totalPassedMethods
                                    + suite_totalFailedMethods + suite_totalSkippedMethods)) * 100);
                } catch (NumberFormatException e)
                {
                    e.printStackTrace();
                }
            }
        }

        StringBuilder dashboardResults = new StringBuilder();

        dashboardResults.append("<table style=\"border-collapse: collapse; width: auto;\">");
        dashboardResults
                .append("<thead><tr> <th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" >Module Name</th><th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" > # Unique TestCases</th><th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" > # Browser Combinations</th><th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" > # Passed</th><th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" > # Failed</th><th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" > # Skipped</th><th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" ># Total</th><th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" >Success Rate</th></tr> </thead> <tbody>");

        int total_browser_combinations = 0;
        int total_unique_testcases = 0;

        for (String key : dashboardReportMap.keySet())
        {

            String fileName = key.trim() + "-Overall" + "-customized-report.html";
            try
            {
                generateModuleOverallTestReport(testName, key, suites, fileName);
            } catch (Exception e)
            {
                e.printStackTrace();
            }

            String value = dashboardReportMap.get(key);
            String[] values = value.split("#");

            int testcase_count = 0;
            int pass_count = 0;
            int fail_count = 0;
            int skip_count = 0;
            int skip_conf_count = 0;
            int fail_conf_count = 0;

            String dashboardModule = key;

            for (String val : values)
            {

                String[] tokens = val.split("\\|");
                for (String token : tokens)
                {
                    if (token.contains("testcase_count"))
                    {
                        testcase_count = testcase_count + Integer.parseInt(token.split("~")[1]);
                    }
                    if (token.contains("pass_count"))
                    {
                        pass_count = pass_count + Integer.parseInt(token.split("~")[1]);
                    }
                    if (token.contains("fail_count"))
                    {
                        fail_count = fail_count + Integer.parseInt(token.split("~")[1]);
                    }
                    if (token.contains("skip_count"))
                    {
                        skip_count = skip_count + Integer.parseInt(token.split("~")[1]);
                    }
                    if (token.contains("skip_conf_count"))
                    {
                        skip_conf_count = skip_conf_count + Integer.parseInt(token.split("~")[1]);
                    }
                    if (token.contains("fail_conf_count"))
                    {
                        fail_conf_count = fail_conf_count + Integer.parseInt(token.split("~")[1]);
                    }
                }
            }

            String[] sub = value.split("#");
            String temp = "";
            for (String s : sub)
            {
                s = s.substring(0, s.indexOf("fail_count"));
                temp = temp + s;
            }

            temp = temp.substring(0, temp.lastIndexOf("|"));
            temp = temp.replace(" ", "%20");

            NumberFormat nformat = NumberFormat.getInstance();
            nformat.setMaximumFractionDigits(2);
            nformat.setGroupingUsed(true);
            String passPercent = nformat
                    .format(((float) pass_count / (float) (pass_count + fail_count + skip_count)) * 100);

            String finalStr = "[";
            String[] val = dashboardReportMap.get(key).split("#");

            int unique_testcase = 0;

            int limit = val.length - 1;
            for (int i = 0; i < val.length; i++)
            {
                String testCaseCount = (val[i].split("\\|")[2]).split("~")[1];
                int next = Integer.parseInt(testCaseCount);
                if (next > unique_testcase)
                {
                    unique_testcase = next;
                }
                finalStr = finalStr + testCaseCount + " T * 1 B]";
                if (i != limit)
                {
                    finalStr += " + [";
                }
            }

            String finalString = "";
            if ((unique_testcase * values.length) != (pass_count + fail_count + skip_count))
            {
                finalString = "<a href=\"#\" title=\"" + finalStr + "\">" + (pass_count + fail_count + skip_count)
                        + "</a>";
            } else
            {
                finalString = String.valueOf((pass_count + fail_count + skip_count));
            }

            String passCount = "";
            String failCount = "";
            String skipCount = "";

            if (pass_count > 0)
            {
                passCount = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #00CC00; font-family: Georgia;\"><b>"
                        + pass_count + "</b></td>";
            } else
            {
                passCount = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                        + pass_count + "</td>";
            }

            if (fail_count > 0)
            {
                failCount = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #CC0000; font-family: Georgia;\"><b>"
                        + fail_count + "</b></td>";
            } else
            {
                failCount = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                        + fail_count + "</td>";
            }

            if (skip_count > 0)
            {
                skipCount = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #CCA300; font-family: Georgia;\"><b>"
                        + skip_count + "</b></td>";
            } else
            {
                skipCount = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                        + skip_count + "</td>";
            }

            dashboardResults
                    .append("<tr><td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\"><b><a href='"
                            + fileName
                            + "'>"
                            + dashboardModule
                            + "</a></b><td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                            + unique_testcase
                            + "</td><td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                            + values.length
                            + "</td>"
                            + passCount
                            + failCount
                            + skipCount
                            + "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                            + finalString
                            + "</td><td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\"><font color=\"#A35200\"><b>"
                            + passPercent + " %" + "</b></font></td></tr>");

            if (total_browser_combinations < values.length)
            {
                total_browser_combinations = values.length;
            }

            total_unique_testcases += unique_testcase;
        }

        dashboardResults.append("</tbody></table>");

        String suite_pass = "";
        String suite_fail = "";
        String suite_skip = "";

        if (suite_totalPassedMethods > 0)
        {
            suite_pass = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #00CC00; font-family: Georgia;\"><b>"
                    + suite_totalPassedMethods + "</b></td>";
        } else
        {
            suite_pass = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                    + suite_totalPassedMethods + "</td>";
        }

        if (suite_totalFailedMethods > 0)
        {
            suite_fail = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #CC0000; font-family: Georgia;\"><b>"
                    + suite_totalFailedMethods + "</b></td>";
        } else
        {
            suite_fail = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                    + suite_totalFailedMethods + "</td>";
        }

        if (suite_totalSkippedMethods > 0)
        {
            suite_skip = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #CCA300; font-family: Georgia;\"><b>"
                    + suite_totalSkippedMethods + "</b></td>";
        } else
        {
            suite_skip = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                    + suite_totalSkippedMethods + "</td>";
        }

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        nf.setGroupingUsed(true);

        try
        {
            suite_passPercentage = nf
                    .format(((float) suite_totalPassedMethods / (float) (total_unique_testcases) * 100));
        } catch (NumberFormatException e)
        {
            e.printStackTrace();
        }

        // Summary Table
        f_out.println("<p><b>Overall Execution Summary</b></p>");
        f_out.println("<table style=\"border-collapse: collapse; width: auto;\"><thead><tr><th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" >Test Suite Name</th><th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" ># Unique TestCases</th><th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" ># Browser Combinations</th> <th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" ># Passed</th> <th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" ># Failed</th> <th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" ># Skipped</th><th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" ># Total</th> <th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" >Success Rate</th> </tr> </thead> <tbody> <tr><td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\"><b>"
                + suiteName
                + "</b></td><td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                + total_unique_testcases
                + "</td><td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                + total_browser_combinations
                + "</td>"
                + suite_pass
                + suite_fail
                + suite_skip
                + "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                + (suite_totalPassedMethods + suite_totalFailedMethods + suite_totalSkippedMethods)
                + "</td><td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\"><font color=\"#A35200\"><b>"
                + suite_passPercentage + " %" + "</b></font></td></tr></tbody></table>");

        f_out.flush();

        f_out.println("<br/>");
        f_out.println("<p><b>Modulewise Execution Summary</b></p>");
        f_out.println(dashboardResults);

        f_out.flush();
    }

    private void generateModuleOverallTestReport(String testName, String moduleVar, List<ISuite> suites,
            String newFileName) throws Exception
    {
        StringBuilder moduleResults = new StringBuilder();

        final PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(new File(outputDir, newFileName))));
        startHtmlPage(pw);

        pw.println("<button onClick=\"location.href='customized-test-run-report.html'\"><span class=\"prev\">Back to Overall Execution Summary</span></button>");
        pw.println("<br/><br/><br/>");
        pw.println("<p><b>Testwise Overall Execution Details</b></p>");
        pw.println("<table style=\"border-collapse: collapse; width: auto;\"><tr><td style=\"text-align: center; border: 0px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">Module Name: </td><td style=\"text-align: center; border: 0px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                + moduleVar + "</td></tr></table><br/><br/>");

        moduleResults.append("<table style=\"border-collapse: collapse; width: auto;\">");
        moduleResults
                .append("<thead><tr><th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" >Test Name</th> <th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" >Module</th> <th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" >Group</th> <th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" >Browser</th> <th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" >Version</th> <th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" >OS</th><th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" >Node IP</th><th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" ># Passed</th> <th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" ># Failed</th> <th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" ># Skipped</th><th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" ># Total</th> <th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" >Success Rate</th> </tr> </thead> <tbody>");

        int totalPassedMethods = 0;
        int totalFailedMethods = 0;
        int totalSkippedMethods = 0;

        String passPercentage = "";

        ITestContext overview = null;

        for (ISuite suite : suites)
        {
            Map<String, ISuiteResult> tests = suite.getResults();

            for (ISuiteResult r : tests.values())
            {
                overview = r.getTestContext();
                testName = overview.getName();

                totalPassedMethods = overview.getPassedTests().getAllMethods().size();
                totalFailedMethods = overview.getFailedTests().getAllMethods().size();
                totalSkippedMethods = overview.getSkippedTests().getAllMethods().size();

                NumberFormat nf = NumberFormat.getInstance();
                nf.setMaximumFractionDigits(2);
                nf.setGroupingUsed(true);
                try
                {
                    passPercentage = nf.format(((float) totalPassedMethods / (float) (totalPassedMethods
                            + totalFailedMethods + totalSkippedMethods)) * 100);
                } catch (NumberFormatException e)
                {
                    e.printStackTrace();
                }

                String includedModule = "";
                String includedGroup = "";

                ITestNGMethod[] allTestMethods = overview.getAllTestMethods();
                for (ITestNGMethod testngMethod : allTestMethods)
                {
                    String[] modules = testngMethod.getGroups();
                    for (String module : modules)
                    {
                        for (String moduleName : MODULES)
                        {
                            if (module.equalsIgnoreCase(moduleName))
                            {
                                if (!(includedModule.contains(module)))
                                {
                                    includedModule = includedModule + " " + module;
                                }
                            }
                        }
                        for (String groupName : TEST_GROUPS)
                        {
                            if (module.equalsIgnoreCase(groupName))
                            {
                                if (!(includedGroup.contains(module)))
                                {
                                    includedGroup = includedGroup + " " + module;
                                }
                            }
                        }
                    }
                }

                String browser = overview.getCurrentXmlTest().getParameter("browser");
                String browser_version = overview.getCurrentXmlTest().getParameter("browser_version");
                String platform = overview.getCurrentXmlTest().getParameter("os");
                String platformVersion = overview.getCurrentXmlTest().getParameter("os_version");

                if (platform == null || platform.trim().length() == 0)
                {
                    platform = "N/A";
                }

                if (browser_version == null || browser_version.trim().length() == 0)
                {
                    browser_version = "N/A";
                }

                if (browser == null || browser.trim().length() == 0)
                {
                    browser = "N/A";
                }

                if (browser.equalsIgnoreCase("firefox"))
                {
                    browser = "Firefox";
                } else if (browser.equalsIgnoreCase("chrome"))
                {
                    browser = "Chrome";
                } else if (browser.equalsIgnoreCase("internet explorer"))
                {
                    browser = "IE";
                }

                if (platform.equalsIgnoreCase("windows") && platformVersion.equalsIgnoreCase("xp"))
                {
                    platform = "XP";
                } else if (platform.equalsIgnoreCase("windows") && platformVersion.equalsIgnoreCase("7"))
                {
                    platform = "Win 7";
                } else if (platform.equalsIgnoreCase("windows") && platformVersion.equalsIgnoreCase("8"))
                {
                    platform = "Win 8";
                } else if (platform.equalsIgnoreCase("mac"))
                {
                    platform = "Mac";
                } else
                {

                }

                if (includedModule.equalsIgnoreCase(moduleVar))
                {
                    String fileName = testName + "-customized-report.html";

                    try
                    {
                        generateModuleWiseTestReport(testName, suites, fileName, moduleVar, "?");
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }

                    String passCount = "";
                    String failCount = "";
                    String skipCount = "";

                    if (totalPassedMethods > 0)
                    {
                        passCount = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #00CC00; font-family: Georgia;\"><b>"
                                + totalPassedMethods + "</b></td>";
                    } else
                    {
                        passCount = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                                + totalPassedMethods + "</td>";
                    }

                    if (totalFailedMethods > 0)
                    {
                        failCount = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #CC0000; font-family: Georgia;\"><b>"
                                + totalFailedMethods + "</b></td>";
                    } else
                    {
                        failCount = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                                + totalFailedMethods + "</td>";
                    }

                    if (totalSkippedMethods > 0)
                    {
                        skipCount = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #CCA300; font-family: Georgia;\"><b>"
                                + totalSkippedMethods + "</b></td>";
                    } else
                    {
                        skipCount = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                                + totalSkippedMethods + "</td>";
                    }

                    moduleResults
                            .append("<tr><td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\"><b><a href=\""
                                    + fileName
                                    + "\">"
                                    + testName
                                    + "</a></b></td>"
                                    + "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                                    + includedModule
                                    + "</td><td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                                    + includedGroup
                                    + "</td><td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                                    + browser
                                    + "</td><td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                                    + browser_version
                                    + "</td><td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                                    + platform
                                    + "</td><td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                                    + "?"
                                    + "</td>"
                                    + passCount
                                    + failCount
                                    + skipCount
                                    + "</td><td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                                    + (totalPassedMethods + totalFailedMethods + totalSkippedMethods)
                                    + "</td><td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\"><font color=\"#A35200\"><b>"
                                    + passPercentage + " %" + "</b></font></td></tr>");
                }
            }
        }

        moduleResults.append("</tbody></table>");
        pw.println(moduleResults);

        endHtmlPage(pw);

        pw.flush();
        pw.close();
    }

    private void generateModuleWiseTestReport(String testName, List<ISuite> suites, String newFileName,
            String passedModule, String nodeIp) throws IOException
    {
        final PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(new File(outputDir, newFileName))));

        startHtmlPage(pw);

        pw.println("<button onClick=\"location.href='" + passedModule + "-Overall-customized-report.html"
                + "'\"><span class=\"prev\">Back to Modulewise Test Execution Summary</span></button>");

        pw.println("<br/><br/><br/>");
        pw.println("<p><b>Modulewise Execution Summary</b></p>");
        pw.println("<table style=\"border-collapse: collapse; width: auto;\"><tr><td style=\"text-align: left; border: 0px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">Test Name: </td><td style=\"text-align: left; border: 0px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                + testName + "</td></tr></table></br/>");

        pw.println("<table style=\"border-collapse: collapse; width: auto;\">");
        pw.println("<thead><tr><th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" >Module Name</th><th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" ># Passed</th><th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" ># Failed</th><th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" ># Skipped</th><th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" ># Total</th><th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" >Success Rate</th></tr></thead>");

        HashMap<String, ArrayList<ITestNGMethod>> moduleMap = new HashMap<String, ArrayList<ITestNGMethod>>();

        ITestContext overview = null;

        for (ISuite suite : suites)
        {
            Map<String, ISuiteResult> tests = suite.getResults();
            for (ISuiteResult r : tests.values())
            {
                overview = r.getTestContext();
                if ((overview.getName()).equalsIgnoreCase(testName))
                {
                    ITestNGMethod[] testngMethods = overview.getAllTestMethods();

                    ArrayList<HashMap<String, ITestNGMethod>> moduleMethods = new ArrayList<HashMap<String, ITestNGMethod>>();

                    for (ITestNGMethod testngMethod : testngMethods)
                    {
                        String[] groups = testngMethod.getGroups();
                        for (String group : groups)
                        {
                            for (String module : MODULES)
                            {
                                if (group.equalsIgnoreCase(module))
                                {
                                    HashMap<String, ITestNGMethod> tempMap = new HashMap<String, ITestNGMethod>();
                                    tempMap.put(module, testngMethod);
                                    moduleMethods.add(tempMap);
                                }
                            }
                        }
                    }

                    for (String module : MODULES)
                    {
                        ArrayList<ITestNGMethod> moduleTestMethods = new ArrayList<ITestNGMethod>();

                        Iterator<HashMap<String, ITestNGMethod>> it = moduleMethods.iterator();

                        while (it.hasNext())
                        {
                            String moduleName = "";
                            ITestNGMethod testMethod = null;

                            HashMap<String, ITestNGMethod> moduleWithTestMethod = it.next();
                            if (moduleWithTestMethod.containsKey(module))
                            {
                                moduleName = module;
                                testMethod = moduleWithTestMethod.get(module);
                            }

                            if (module.equalsIgnoreCase(moduleName))
                            {
                                moduleTestMethods.add(testMethod);
                            }
                        }

                        moduleMap.put(module, moduleTestMethods);
                    }
                }
            }
        }

        Set<String> keySet = moduleMap.keySet();
        Iterator<String> it = keySet.iterator();

        for (ISuite suite : suites)
        {
            Map<String, ISuiteResult> tests = suite.getResults();
            for (ISuiteResult r : tests.values())
            {
                overview = r.getTestContext();
                if ((overview.getName()).equalsIgnoreCase(testName))
                {
                    while (it.hasNext())
                    {
                        String moduleName = (String) it.next();

                        int totalPassedMethods = 0;
                        int totalFailedMethods = 0;
                        int totalSkippedMethods = 0;
                        int totalSkippedConfigurations = 0;
                        int totalFailedConfigurations = 0;

                        ArrayList<ITestNGMethod> values = moduleMap.get(moduleName);
                        ListIterator<ITestNGMethod> it2 = values.listIterator();

                        while (it2.hasNext())
                        {
                            ITestNGMethod method = it2.next();

                            int failedMethods = overview.getFailedTests().getResults(method).size();
                            int passedMethods = overview.getPassedTests().getResults(method).size();
                            int skippedMethods = overview.getSkippedTests().getResults(method).size();

                            totalPassedMethods += passedMethods;
                            totalFailedMethods += failedMethods;
                            totalSkippedMethods += skippedMethods;

                        }

                        if (values.size() > 0)
                        {
                            String fileName = testName + "-" + moduleName + "-customized-report.html";
                            try
                            {
                                generateModuleTestMethodSummary(testName, moduleName, suites, fileName, values, nodeIp);
                            } catch (IOException e)
                            {
                                e.printStackTrace();
                            }

                            int totalMethods = totalPassedMethods + totalFailedMethods + totalSkippedMethods;

                            NumberFormat nf = NumberFormat.getInstance();
                            nf.setMaximumFractionDigits(2);
                            nf.setGroupingUsed(false);

                            String passPercentage = nf
                                    .format(((float) totalPassedMethods / (float) totalMethods) * 100);

                            generateModulesRow(pw, fileName, moduleName, totalPassedMethods, totalFailedMethods,
                                    totalSkippedMethods, totalSkippedConfigurations, totalFailedConfigurations,
                                    totalMethods, passPercentage);
                        }
                    }
                    break;
                }
            }
        }

        pw.println("</table>");
        endHtmlPage(pw);
        pw.flush();
        pw.close();
    }

    private void generateModuleTestMethodSummary(String testName, String modulename, List<ISuite> suites,
            String fileName, ArrayList<ITestNGMethod> testngMethods, String nodeIp) throws IOException
    {
        final PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(new File(outputDir, fileName))));

        startHtmlPage(pw);

        String htmlFile = testName + "-customized-report.html";
        String modulewiseTestFileName = testName + "-" + modulename + "-customized-report.html";

        pw.println("<button onClick=\"location.href='" + htmlFile
                + "'\"><span class=\"prev\">Back to Modulewise Execution Summary</span></button>");
        pw.println("<br/><br/><br/>");
        pw.println("<p><b>Details</b></p>");
        pw.println("<table style=\"border-collapse: collapse; width: auto;\"><tr><td style=\"text-align: left; border: 0px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">Test Name: </td><td style=\"text-align: left; border: 0px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                + testName + "</td></tr>");
        pw.println("<tr><td style=\"text-align: left; border: 0px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">Module Name: </td><td style=\"text-align: left; border: 0px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                + modulename + "</td></tr></table></br/>");

        pw.println("<table style=\"border-collapse: collapse; width: auto;\">");
        pw.println("<thead><tr><th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" >Method Name</th><th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" >Total Time (ms)</th><th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\" >Status</th><th style=\"background-color: #93b874; border: 1px solid grey; height: 25px; color: white; font-family: Georgia;\">Stack Trace | Test Parameters</th></tr></thead>");

        for (ISuite suite : suites)
        {
            Map<String, ISuiteResult> tests = suite.getResults();
            for (ISuiteResult re : tests.values())
            {
                ITestContext overview = re.getTestContext();
                if ((overview.getName()).equalsIgnoreCase(testName))
                {
                    Iterator<ITestNGMethod> it = testngMethods.iterator();
                    while (it.hasNext())
                    {
                        ITestNGMethod method = it.next();
                        String[] allGroups = method.getGroups();

                        String methodName = "";
                        String className = "";

                        for (String grp : allGroups)
                        {
                            if (grp.equalsIgnoreCase(modulename))
                            {
                                methodName = method.getMethodName();
                                className = method.getTestClass().getName();

                                ArrayList<Set<ITestResult>> statusResult = new ArrayList<Set<ITestResult>>();

                                Set<ITestResult> failedTestStatus = overview.getFailedTests().getResults(method);
                                if (!(failedTestStatus.isEmpty()))
                                {
                                    statusResult.add(failedTestStatus);
                                }

                                Set<ITestResult> passedTestStatus = overview.getPassedTests().getResults(method);
                                if (!(passedTestStatus.isEmpty()))
                                {
                                    statusResult.add(passedTestStatus);
                                }

                                Set<ITestResult> skippedTestStatus = overview.getSkippedTests().getResults(method);
                                if (!(skippedTestStatus.isEmpty()))
                                {
                                    statusResult.add(skippedTestStatus);
                                }

                                pw.println("<tr><td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\"><b>"
                                        + method.getMethodName() + "</b></td></tr>");
                                Iterator<Set<ITestResult>> statusIterator = statusResult.iterator();

                                while (statusIterator.hasNext())
                                {
                                    Set<ITestResult> status = statusIterator.next();

                                    StringBuilder stackTrace;
                                    StringBuilder failedConf;

                                    Iterator<ITestResult> it2 = status.iterator();

                                    List<String> msgs = new ArrayList<String>();

                                    String executionStatus = "";

                                    long time_start = Long.MAX_VALUE;
                                    long time_end = Long.MIN_VALUE;

                                    Throwable exception = null;
                                    String screenshotFileLink = "";

                                    ITestResult result = null;

                                    while (it2.hasNext())
                                    {
                                        stackTrace = new StringBuilder();
                                        failedConf = new StringBuilder();

                                        result = it2.next();

                                        time_start = result.getStartMillis();
                                        time_end = result.getEndMillis();

                                        int execStatus = result.getStatus();
                                        if (execStatus == ITestResult.SUCCESS)
                                        {
                                            executionStatus = "PASS";
                                        } else if (execStatus == ITestResult.FAILURE)
                                        {
                                            executionStatus = "FAIL";
                                        } else if (execStatus == ITestResult.SKIP)
                                        {
                                            executionStatus = "SKIP";
                                        }

                                        if (execStatus == ITestResult.SKIP)
                                        {
                                            status = overview.getFailedConfigurations().getAllResults();
                                            it2 = status.iterator();
                                            failedConf.append("<br/>");
                                            while (it2.hasNext())
                                            {
                                                result = it2.next();
                                                failedConf.append("Failed Configuration - "
                                                        + result.getMethod().getMethodName());
                                                failedConf.append("<br/>");
                                            }
                                            exception = result.getThrowable();
                                        } else
                                        {
                                            exception = result.getThrowable();
                                        }

                                        try
                                        {
                                            msgs = Reporter.getOutput(result);

                                            /*
                                             * if (msgs.size() == 0) { msgs =
                                             * Reporter.getOutput(); }
                                             */
                                        } catch (Exception ex)
                                        {
                                            // Log error message
                                        }

                                        /*
                                         * If enable logs is false, then only
                                         * take the screenshot.
                                         */
                                        try
                                        {
                                            if ((PropertyUtils.getProperty("app.enable.logs.in.report")
                                                    .equalsIgnoreCase("false")) && (msgs != null))
                                            {
                                                for (String line : msgs)
                                                {
                                                    if (line.contains("[Console Log] Screenshot saved in"))
                                                    {
                                                        screenshotFileLink = line.substring(line.indexOf("in") + 3,
                                                                line.length());
                                                        break;
                                                    }
                                                }
                                            }
                                        } catch (Exception ex)
                                        {
                                            ex.printStackTrace();
                                        }

                                        /*
                                         * If enable logs is true, take the
                                         * whole log along with screenshot.
                                         */
                                        try
                                        {
                                            if ((PropertyUtils.getProperty("app.enable.logs.in.report")
                                                    .equalsIgnoreCase("true")) && (msgs != null))
                                            {
                                                for (String line : msgs)
                                                {
                                                    if (line.contains("[Console Log] Screenshot saved in"))
                                                    {
                                                        screenshotFileLink = line.substring(line.indexOf("in") + 3,
                                                                line.length());
                                                        break;
                                                    }
                                                }

                                                if (screenshotFileLink.trim().length() != 0)
                                                {
                                                    stackTrace
                                                            .append("<br/><a target=\"_blank\" href=\""
                                                                    + screenshotFileLink
                                                                    + "\"><b>View Screenshot in New Window/Tab</b></a><br/><br/><img id=\"screenshot\" src='"
                                                                    + screenshotFileLink
                                                                    + "' height='300' width='300' border=\"1\" style=\"position: relative; left: 0px;\"/>");
                                                }

                                                for (String line : msgs)
                                                {
                                                    if (!(line.contains("[Console Log] Screenshot saved in")))
                                                    {
                                                        stackTrace.append("<br/>" + line);
                                                    }
                                                }
                                            }
                                        } catch (Exception ex)
                                        {
                                            ex.printStackTrace();
                                        }

                                        if (msgs != null)
                                        {
                                            msgs.clear();
                                        }

                                        Random randomGenerator = new Random();
                                        int randomInt = randomGenerator.nextInt(100000);

                                        String stackTraceFile = testName + "-" + modulename + "-" + methodName + "-"
                                                + randomInt + "-" + "custom-report.html";

                                        stackTrace.append("<br/>" + failedConf.toString());

                                        generateStackTraceReport(modulewiseTestFileName, stackTraceFile, stackTrace,
                                                exception, method, nodeIp, result);

                                        String link = "<button onClick=\"location.href='" + stackTraceFile
                                                + "'\"><span class=\"info\">" + "View StackTrace/Params"
                                                + "</span></button>";

                                        if (executionStatus.equalsIgnoreCase("pass"))
                                        {
                                            executionStatus = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #00CC00; font-family: Georgia;\"><b>"
                                                    + executionStatus + "</b></td>";
                                        } else if (executionStatus.equalsIgnoreCase("fail"))
                                        {
                                            executionStatus = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #CC0000; font-family: Georgia;\"><b>"
                                                    + executionStatus + "</b></td>";
                                        } else if (executionStatus.equalsIgnoreCase("skip"))
                                        {
                                            executionStatus = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #CCA300; font-family: Georgia;\"><b>"
                                                    + executionStatus + "</b></td>";
                                        } else
                                        {
                                            executionStatus = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                                                    + executionStatus + "</td>";
                                        }

                                        pw.println("<tr><td style=\"text-align:left\">"
                                                + "[Class Name] "
                                                + className
                                                + "</td><td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                                                + (time_end - time_start)
                                                + "</td>"
                                                + executionStatus
                                                + "<td style=\"text-align: center; border: 0px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                                                + link + "</td></tr>");
                                        pw.flush();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        pw.println("</table>");
        endHtmlPage(pw);
        pw.flush();
        pw.close();
    }

    private void generateStackTraceReport(String fileName, String stackTraceFile, StringBuilder stackTrace,
            Throwable exception, ITestNGMethod method, String nodeIp, ITestResult result) throws IOException
    {
        final PrintWriter fw = new PrintWriter(new BufferedWriter(new FileWriter(new File(outputDir, stackTraceFile))));
        startHtmlPage(fw);

        fw.println("<button  onClick=\"location.href='" + fileName
                + "'\"><span class=\"prev\">Back to Methodwise Execution Summary</span></button>");
        fw.println("<br/><br/><br/>");

        if (result != null)
        {
            fw.println("<fieldset><legend><font color=\"blue\"><b>Test Parameters</b></font></legend>");
            fw.println("<table style=\"border-collapse: collapse; width: auto;\">");

            Object[] params = result.getParameters();
            if (params != null)
            {
                for (int i = 0; i < params.length; i++)
                {
                    fw.println("<tr><td style=\"text-align: left; border: 0px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">[Parameter "
                            + i
                            + "]</td><td style=\"text-align: left; border: 0px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                            + params[i] + "</td></tr>");
                }
            }

            fw.println("</table>");
            fw.println("</fieldset>");
            fw.println("<br/>");
        }

        fw.println("<fieldset><legend><font color=\"green\"><b>Screenshot / Exception Log</b></font></legend>");
        try
        {
            if (PropertyUtils.getProperty("app.enable.logs.in.report").equalsIgnoreCase("false"))
            {
                fw.println("<p><b>[Debug] - </b>Console logs in custom report is disabled. To view the Console logs please set the app.enable.logs.in.report as true in properties file!</p>");
            }
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

        // fw.println("<p><b>[Node IP] - </b>" + nodeIp + "</p>");
        fw.println(stackTrace + "<br/>");
        fw.println("</fieldset>");
        fw.println("<br/>");

        if (exception != null)
        {
            fw.println("<fieldset><legend><font color=\"red\"><b>Stack Trace</b></font></legend>");
            generateExceptionReport(exception, method, fw);
            fw.println("</fieldset>");
        }

        endHtmlPage(fw);
        fw.flush();
        fw.close();
    }

    protected void generateExceptionReport(Throwable exception, ITestNGMethod method, PrintWriter pw)
    {
        pw.flush();
        generateExceptionReport(exception, method, exception.getLocalizedMessage(), pw);
    }

    private void generateExceptionReport(Throwable exception, ITestNGMethod method, String title, PrintWriter m_out)
    {

        m_out.println("<p>" + title + "</p><p>");

        StackTraceElement[] s1 = exception.getStackTrace();
        Throwable t2 = exception.getCause();
        if (t2 == exception)
        {
            t2 = null;
        }
        int maxlines = Math.min(100, StackTraceTools.getTestRoot(s1, method));
        for (int x = 0; x <= maxlines; x++)
        {
            m_out.println((x > 0 ? "<br/>at " : "") + Utils.escapeHtml(s1[x].toString()));
        }
        if (maxlines < s1.length)
        {
            m_out.println("<br/>" + (s1.length - maxlines) + " lines not shown");
        }
        if (t2 != null)
        {
            generateExceptionReport(t2, method, "Caused by " + t2.getLocalizedMessage(), m_out);
        }
        m_out.println("</p>");
        m_out.flush();
    }

    private void generateModulesRow(PrintWriter pw, String fileName, String moduleName, int passedMethods,
            int failedMethods, int skippedMethods, int skippedConfiguration, int failedConfiguration, int totalMethods,
            String passPercentage)
    {

        String passCount = "";
        String failCount = "";
        String skipCount = "";

        if (passedMethods > 0)
        {
            passCount = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #00CC00; font-family: Georgia;\"><b>"
                    + passedMethods + "</b></td>";
        } else
        {
            passCount = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                    + passedMethods + "</td>";
        }

        if (failedMethods > 0)
        {
            failCount = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #CC0000; font-family: Georgia;\"><b>"
                    + failedMethods + "</b></td>";
        } else
        {
            failCount = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                    + failedMethods + "</td>";
        }

        if (skippedMethods > 0)
        {
            skipCount = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #CCA300; font-family: Georgia;\"><b>"
                    + skippedMethods + "</b></td>";
        } else
        {
            skipCount = "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                    + skippedMethods + "</td>";
        }

        pw.println("<tr><td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\"><b><a href=\""
                + fileName
                + "\">"
                + moduleName
                + "</a></b></td>"
                + passCount
                + failCount
                + skipCount
                + "<td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                + totalMethods
                + "</td><td style=\"text-align: center; border: 1px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\"><font color=\"#A35200\"><b>"
                + passPercentage + " %" + "</b></font></td></tr>");

        pw.flush();
    }

    private void generateAdditionalInfoReport(List<ISuite> suites) throws Exception
    {
        String url = System.getProperty("app.url");

        if (url == null)
        {
            url = PropertyUtils.getProperty("app.url");
        }

        f_out.println("<b><font color=\"#333300\">Test Environment Details</font></b><br/><br/>");
        f_out.println("<table style=\"border-collapse: collapse; width: 60%;\">");
        // f_out.println("<tr><th style=\"text-align: center; background-color: #93b874; border: 0px solid grey; height: 25px; color: #4c4c4c; font-family: Georgia;\" colspan=\"2\"><b>Configuration Details</b></th></tr>");

        f_out.println("<tr><td style=\"text-align: left; border: 0px solid grey; height: 25px;width:60%; color: #1C1C1C; font-family: Georgia;\">Application URL: </td><td style=\"text-align: left; border: 0px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                + "<a href=\"" + url + "\">" + url + "</a></td></tr>");

        for (ISuite suite : suites)
        {
            f_out.println("<tr><td style=\"text-align: left; border: 0px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">Parallel Run: </td><td style=\"text-align: left; border: 0px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                    + suite.getParallel() + "</td></tr>");
            break;
        }

        f_out.println("</table>");
        f_out.println("<br/>");

        f_out.println("<b><font color=\"#333300\">OS/Browser Details</font></b><br/><br/>");
        f_out.println("<table style=\"border-collapse: collapse; width: 60%;\">");
        // f_out.println("<tr><th style=\"text-align: center; background-color: #93b874; border: 0px solid grey; height: 25px; color: #4c4c4c; font-family: Georgia;\" colspan=\"2\"><b>Configuration Details</b></th></tr>");

        f_out.println("<tr><td style=\"text-align: left; border: 0px solid grey; height: 25px;width:60%; color: #1C1C1C; font-family: Georgia;\">OS Name: </td><td style=\"text-align: left; border: 0px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                + "" + WordUtils.capitalize(System.getProperty("os")) + "</td></tr>");

        f_out.println("<tr><td style=\"text-align: left; border: 0px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">OS Version: </td><td style=\"text-align: left; border: 0px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                + "" + System.getProperty("os_version") + "</td></tr>");

        f_out.println("<tr><td style=\"text-align: left; border: 0px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">Browser Name: </td><td style=\"text-align: left; border: 0px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                + "" + WordUtils.capitalize(System.getProperty("browser")) + "</td></tr>");

        f_out.println("<tr><td style=\"text-align: left; border: 0px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">Browser Version: </td><td style=\"text-align: left; border: 0px solid grey; height: 25px; color: #1C1C1C; font-family: Georgia;\">"
                + "" + System.getProperty("browser_version") + "</td></tr>");

        f_out.println("</table>");
        f_out.println("<br/>");

        f_out.flush();
    }

    private PrintWriter createWriter(String outdir) throws IOException
    {
        new File(outdir).mkdirs();
        return new PrintWriter(new BufferedWriter(
                new FileWriter(new File(outputDir, "customized-test-run-report.html"))));
    }

    /** Starts HTML Stream */
    private void startHtmlPage(PrintWriter out)
    {
        out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">");
        out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
        out.println("<head>");
        out.println("<title>My Company QA Automation Test Results Summary</title>");
        out.println("</head>");
        out.println("<body><div style=\"margin:0 auto; padding:15px; min-height:450px; min-width: 450px; height:auto;\">"
                + "<div style=\"height:auto; background: #ded;padding:20px;box-shadow: 0 10px 6px -6px #777 \">"
                + "<h1 style=\"background-color: #93b874; color: white; text-align: center; font-family: Georgia;\">My Company QA Automation Report</h1>");

        Calendar currentdate = Calendar.getInstance();
        String strdate = null;
        DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy hh:mm a z");
        strdate = formatter.format(currentdate.getTime());

        TimeZone obj = TimeZone.getTimeZone("IST");

        formatter.setTimeZone(obj);
        strdate = formatter.format(currentdate.getTime());

        out.println("<br/><div align=\"right\">Report generated on: " + strdate + "</div><br/><br/>");

        out.flush();
    }

    /** Finishes HTML Stream */
    private void endHtmlPage(PrintWriter out)
    {
        out.println("<br/><br/><div align=\"right\"> &copy; <a href=\"http://www.mycompany.com\">2014 My Company Ltd.</a></div>");
        out.println("</body></html>");
    }
}
