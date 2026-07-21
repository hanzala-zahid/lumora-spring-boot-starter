# Lumora Spring Boot Starter

The `lumora-spring-boot-starter` provides zero-configuration observability for Spring Boot applications. It automatically intercepts, formats, and streams application logs, metrics, and exceptions directly to your HanzWare Lumora platform for real-time monitoring and AI-powered analysis.

## Features
- **Auto-Configuration:** Simply drop the dependency in your project and provide your API keys. No code changes required.
- **Log Streaming:** Real-time log ingestion directly into the Lumora dashboard.
- **Exception Interception:** Automatically captures full stack traces and context for unhandled exceptions.
- **AI-Ready:** Seamlessly connects your logs to **Lumora AI** for instant error explanations, root-cause analysis, and project health recommendations.
- **Cloud Native:** By default, telemetry is sent directly to the cloud without requiring a local agent (though the Lumora Agent is supported for advanced OS metrics).

---

## 1. Installation

Since this library is distributed via JitPack for easy testing, you must first add the JitPack repository to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Then, add the starter dependency to your project:

```xml
<dependency>
    <groupId>com.github.hanzala-zahid</groupId>
    <artifactId>lumora-spring-boot-starter</artifactId>
    <version>v1.0.0</version>
</dependency>
```

---

## 2. Configuration

To connect your application to your Lumora workspace, add your project credentials to your `application.properties` (or `application.yml`):

```properties

# Required: Your unique Lumora Project ID
lumora.project.id=YOUR_PROJECT_ID

# Required: The API key for your project (available in the HanzWare Dashboard)
lumora.api.key=YOUR_API_KEY

# Optional: The base URL of the Lumora ingestion endpoint (defaults to cloud)
lumora.url=https://api.hanzware.com/lumora/ingest

---

## 3. How It Works

Once your application starts, the `LumoraAutoConfiguration` class automatically injects a custom `LogbackAppender` into your logging pipeline. 

Any log emitted by your application (via SLF4J/Logback) will be asynchronously bundled and shipped to your Lumora instance in the background. It will automatically serialize standard fields, stack traces, thread names, and log levels.

### Example Usage
You do not need to use any special Lumora SDK classes in your code. Standard Spring logging works out of the box:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {
    private static final Logger log = LoggerFactory.getLogger(DemoController.class);

    @GetMapping("/test")
    public String testLogging() {
        log.info("This info log will appear in Lumora instantly!");
        
        try {
            throw new RuntimeException("Simulated Database Failure");
        } catch (Exception e) {
            // This stack trace will be captured and available for AI explanation
            log.error("An error occurred during processing", e);
        }
        
        return "Success";
    }
}
```

---

## 4. Using Lumora AI

When you view your streamed logs in the HanzWare Dashboard, look for the **"AI Explain"** button next to any `ERROR` or `WARN` logs. Lumora AI will automatically ingest the stack trace context and provide a plain-English explanation along with actionable remediation steps.

---

## 5. The Optional Lumora Agent

The Lumora Agent is **totally optional**. The Spring Boot Starter is capable of sending all telemetry directly over HTTP to the cloud. However, if you require offline buffering or deeper OS-level metrics (CPU, RAM, Disk usage), you can install the standalone Lumora Agent on your server.

### Downloading the Agent
You can download the latest agent binaries directly from the [Lumora Agent GitHub Releases](https://github.com/hanzala-zahid/lumora-agent/releases) page.

### Installation Instructions (Windows)
Because the `lumora-agent.exe` is currently an unsigned open-source binary, Windows Defender SmartScreen may flag it as an unrecognized app. This is completely normal for new, unsigned software.

**To run the agent:**
1. Download the `lumora-agent.exe` file.
2. Double-click the `.exe` to run it.
3. If a blue "Windows protected your PC" popup appears, click on **"More info"**, then click **"Run anyway"**.

**If "More info" does not appear:**
1. Right-click the downloaded `lumora-agent.exe` file and select **Properties**.
2. At the bottom of the **General** tab, check the box next to **Unblock**.
3. Click **Apply** and **OK**.
4. Double-click the file again to run the agent.
