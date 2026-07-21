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

If the package is published to GitHub Packages, you must first add the repository to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/hanzala-zahid/lumora-spring-boot-starter</url>
    </repository>
</repositories>
```

Then, add the starter dependency to your project:

```xml
<dependency>
    <groupId>com.hanzware.lumora</groupId>
    <artifactId>lumora-spring-boot-starter</artifactId>
    <version>1.0.0</version>
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
lumora.url=http://localhost:8080/v1/lumora/ingest
```

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

*Note: The Lumora Agent is **totally optional**. The Spring Boot Starter is capable of sending all telemetry directly over HTTP to the cloud. Install the local agent only if you require offline buffering or deeper OS-level metrics.*
