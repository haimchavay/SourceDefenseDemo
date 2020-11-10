package com.example.test;

import com.example.ReportObj;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.proxy.CaptureType;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openqa.selenium.By;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;

@RunWith(Parameterized.class)
public class TestDemo {
    // *** CONFIGURATION PARAMS -
    // Chrome drive file path
    private String chromeDriverPath;
    // URL
    private String site;
    // Creation JSON file path
    private String createFilePath;
    // Cycles of reload page
    private int cycles;
    // Wait when browse to page
    private int waitSec;
    // *****

    // *** Only class Members
    // Selenium web driver
    private WebDriver driver;
    // Chrome driver key
    private final String CHROME_DRIVER_KEY = "webdriver.chrome.driver";
    // Proxy
    private BrowserMobProxy proxy;
    // *****

    public TestDemo(String chromeDriverPath, String site, String createFilePath, int cycles, int waitSec){
        this.chromeDriverPath = chromeDriverPath;
        this.site             = site;
        this.createFilePath   = createFilePath;
        this.cycles           = cycles;
        this.waitSec          = waitSec;
        proxy                 = new BrowserMobProxyServer();
    }

    @Parameterized.Parameters
    public static Collection parameters(){
        // In this part the user can change the values of the params(chromeDriverPath, site, createFilePath, cycles)
        return Arrays.asList(new Object[][]{
                {
                        "C:\\Selenium\\chromedriver_win32\\chromedriver.exe",
                        "https://www.geektime.co.il/source-defense-raises-10-5-m/",
                        "C:\\Users\\haimc\\Desktop\\Languages\\Json\\tmp.json",
                        3,
                        30
                }
        });
    }

    @Before
    public void prepare(){
        System.out.println("*** START prepare ***");
        System.setProperty(CHROME_DRIVER_KEY, chromeDriverPath);

        // ******* GET NETWORK REQUESTS AND WARNING / ERROR LOGS ****
        System.out.println("Going to create selenium proxy");
        // start the proxy
        proxy.start(0);
        // get the Selenium proxy object
        Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);

        System.out.println("Going to configure desired capability");
        // configure it as a desired capability
        DesiredCapabilities caps = DesiredCapabilities.chrome();
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.BROWSER, Level.ALL);
        caps.setCapability(CapabilityType.LOGGING_PREFS, logPrefs);
        caps.setCapability(CapabilityType.PROXY, seleniumProxy);

        System.out.println("Going to create chrome driver object");
        driver = new ChromeDriver(caps);

        // enable more detailed HAR capture, if desired (see CaptureType for the complete list)
        proxy.enableHarCaptureTypes(CaptureType.REQUEST_CONTENT, CaptureType.REQUEST_COOKIES,
                CaptureType.REQUEST_BINARY_CONTENT, CaptureType.REQUEST_HEADERS);
        // create a new HAR with the label "geektime.co.il"
        int begin = site.indexOf("www");
        int end   =site.indexOf("/", begin);
        if(end == -1){
            end = site.length();
        }
        String subUrl = site.substring(begin + 4, end);
        proxy.newHar(subUrl);

        System.out.println("Going to open the browser : " + site);
        // open browser
        driver.get(site);
        System.out.println("*** Prepare successful ***");
    }
    @Test
    public void testCreateJsonReportFile() throws IOException, InterruptedException {
        System.out.println("*** Start testCreateJsonReportFile ***");

        System.out.println("Going to create explicit wait object");
        WebDriverWait wait = new WebDriverWait(driver, 30);

        System.out.println("Going to check if your connection is not private");
        WebElement btn = driver.findElement(By.id("details-button"));
        if(btn != null){
            btn = wait.until(ExpectedConditions.elementToBeClickable(btn));
            btn.click();
            btn = driver.findElement(By.id("proceed-link"));
            btn = wait.until(ExpectedConditions.elementToBeClickable(btn));
            btn.click();
        }

        System.out.println("Going to create object with network requests and console logs");
        ReportObj reportObj[] = new ReportObj[cycles];
        for(int i = 0; i < cycles; i++){
            Thread.sleep(waitSec * 1000);
            reportObj[i] = new ReportObj();

            // get the HAR data
            Har har = proxy.getHar();

            for(int j = 0; j < har.getLog().getEntries().size(); j++){
                net.lightbody.bmp.core.har.HarRequest request = har.getLog().getEntries().get(j).getRequest();
                reportObj[i].getRequestList().add(request);
            }

            LogEntries logEntries = driver.manage().logs().get(LogType.BROWSER);

            reportObj[i].setLogEntries(logEntries);
            driver.navigate().refresh();
        }

        System.out.println("Going to create new json file to " + createFilePath);
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        ow.writeValue(new FileOutputStream(createFilePath), reportObj);

        System.out.println("*** TestCreateJsonReportFile Successful ***");
    }
    @After
    public void cleanup(){
        System.out.println("*** Going to close the driver ***");
        driver.close();
        driver.quit();
        System.out.println("*** Cleanup successful ***");
    }
}
