package lv.ctco.zephyr.transformer;

import lv.ctco.zephyr.ZephyrSyncException;
import lv.ctco.zephyr.beans.TestCase;
import lv.ctco.zephyr.beans.testresult.junit.JUnitResult;
import lv.ctco.zephyr.beans.testresult.junit.JUnitResultTestSuite;
import lv.ctco.zephyr.enums.TestStatus;
import lv.ctco.zephyr.util.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.CanReadFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.util.*;

import static java.util.Collections.singletonList;

public class JUnitTransformer implements ReportTransformer {

    private static final String REGEX = "TEST.*\\.xml";

    public String getType() {
        return "junit";
    }

    public List<TestCase> transformToTestCases(String reportPath) {
        return transform(readJUnitReport(reportPath));
    }

    List<JUnitResultTestSuite> readJUnitReport(String path) {
        File file = new File(path);
        if (file.isDirectory()) {
            return unmarshalSuites(file);
        } else {
            return singletonList(unmarshal(path));
        }
    }

    List<TestCase> transform(List<JUnitResultTestSuite> resultTestSuite) {
        if (resultTestSuite.size() == 0) {
            return new ArrayList<>();
        }

        List<TestCase> result = new ArrayList<>();
        for (JUnitResultTestSuite suite : resultTestSuite) {
            for (JUnitResult testCase : suite.getTestcase()) {
                TestCase test = new TestCase();
                test.setName(testCase.getName());
                test.setUniqueId(generateUniqueId(testCase));
                test.setStatus(testCase.getError() != null || testCase.getFailure() != null ? TestStatus.FAILED : TestStatus.PASSED);
                result.add(test);
            }
        }
        return result;
    }

    private JUnitResultTestSuite unmarshal(String path) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(JUnitResultTestSuite.class);
            return (JUnitResultTestSuite) jaxbContext.createUnmarshaller().unmarshal(new File(path));
        } catch (JAXBException e) {
            throw new ZephyrSyncException("Cannot process JUnit report", e);
        }
    }

    private List<JUnitResultTestSuite> unmarshalSuites(File... directories) {
        List<JUnitResultTestSuite> results = new ArrayList<>();
        List<File> files = listTestSuiteFiles(directories);
        for (File currentFile : files) {
            results.add(unmarshal(currentFile.getPath()));
        }
        return results;
    }

    private List<File> listTestSuiteFiles(File[] directories) {
        List<File> files = new ArrayList<>();
        for (File directory : directories) {
            if (!directory.isDirectory()) {
                continue;
            }
            Collection<File> filesInDirectory = FileUtils.listFiles(directory,
                    new RegexFileFilter(REGEX),
                    CanReadFileFilter.CAN_READ);
            files.addAll(filesInDirectory);
        }
        return files;
    }

    private String generateUniqueId(JUnitResult testCase) {
        return String.join("-", Utils.normalizeKey(testCase.getClassname()), Utils.normalizeKey(testCase.getName()));
    }
}