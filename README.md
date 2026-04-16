# Mule Runtime Support Policy

This document describes the structure and usage of `mule-runtime-support-matrix.json`, a machine-readable reference for Mule Runtime version support lifecycle dates and Java compatibility.

## Overview

The JSON file contains official support dates and Java version compatibility for all Mule Runtime versions extracted from:
- [MuleSoft Product Versioning and Back Support Policy](https://www.salesforce.com/en-us/wp-content/uploads/sites/4/documents/legal/Agreements/versioning-back-support-policy.pdf)
- [Mule Runtime Java Adoption Guide](https://docs.mulesoft.com/release-notes/mule-runtime/java-adoption)

## File Structure

```json
{
  "metadata": {
    "source": "...",
    "source_url": "...",
    "java_adoption_url": "...",
    "extracted_date": "2026-04-16"
  },
  "versions": {
    "4.11": {
      "type": "Edge",
      "release_date": "2026-02-04",
      "end_of_standard_support": "2026-07-04",
      "end_of_extended_support": "2026-10-04",
      "end_of_life": "2027-10-04",
      "supported_java_versions": ["17"]
    }
  }
}
```

## Field Descriptions

### Metadata

| Field | Description |
|-------|-------------|
| `source` | Document name where support dates are published |
| `source_url` | Direct link to the official support policy PDF |
| `java_adoption_url` | Link to Java version compatibility documentation |
| `extracted_date` | Date when this data was extracted (ISO 8601 format) |

### Version Objects

Each version key (e.g., `"4.11"`, `"4.9"`) maps to an object with the following fields:

| Field | Type | Description |
|-------|------|-------------|
| `type` | String | Release type: `"Edge"`, `"LTS"`, or `"Minor"` |
| `release_date` | String | General availability (GA) date (ISO 8601: YYYY-MM-DD) |
| `end_of_standard_support` | String | Last date for Standard Support (ISO 8601) |
| `end_of_extended_support` | String | Last date for Extended Support (ISO 8601) |
| `end_of_life` | String | End of Life date - 12 months after Extended Support ends (ISO 8601) |
| `supported_java_versions` | Array | List of supported Java/JDK major versions (as strings) |

### Release Types

- **Edge**: Short-lived releases with latest features (4-5 months Standard Support)
- **LTS**: Long Term Support releases (18+ months Standard Support)
- **Minor**: Legacy versioning scheme (pre-4.5 releases)

### Support Phases

1. **Standard Support**: Full support including patches for S1/S2 issues and critical security vulnerabilities
2. **Extended Support**: Continued support with limited patch availability (production environments only)
3. **End of Life**: Technical assistance only, no patches provided

## Usage Examples

### JavaScript/Node.js

```javascript
const supportData = require('./mule-runtime-support-matrix.json');

// Get support dates for a specific version
const version = supportData.versions['4.9'];
console.log(`Mule ${version.type} 4.9`);
console.log(`Standard Support ends: ${version.end_of_standard_support}`);
console.log(`Supported Java versions: ${version.supported_java_versions.join(', ')}`);

// Check if a version is currently supported
function isSupported(versionKey) {
  const version = supportData.versions[versionKey];
  const today = new Date().toISOString().split('T')[0];
  return today <= version.end_of_extended_support;
}

console.log(`Is 4.6 still supported? ${isSupported('4.6')}`);

// Find all LTS versions
const ltsVersions = Object.entries(supportData.versions)
  .filter(([_, data]) => data.type === 'LTS')
  .map(([version, _]) => version);
console.log(`LTS versions: ${ltsVersions.join(', ')}`);

// Find versions supporting Java 17
const java17Versions = Object.entries(supportData.versions)
  .filter(([_, data]) => data.supported_java_versions.includes('17'))
  .map(([version, _]) => version);
console.log(`Java 17 compatible: ${java17Versions.join(', ')}`);
```

### Python

```python
import json
from datetime import datetime

with open('mule-runtime-support-matrix.json', 'r') as f:
    support_data = json.load(f)

# Get support dates for a specific version
version = support_data['versions']['4.9']
print(f"Mule {version['type']} 4.9")
print(f"Standard Support ends: {version['end_of_standard_support']}")
print(f"Supported Java versions: {', '.join(version['supported_java_versions'])}")

# Check if a version is currently supported
def is_supported(version_key):
    version = support_data['versions'][version_key]
    today = datetime.now().date().isoformat()
    return today <= version['end_of_extended_support']

print(f"Is 4.6 still supported? {is_supported('4.6')}")

# Find all LTS versions
lts_versions = [
    version_key 
    for version_key, data in support_data['versions'].items()
    if data['type'] == 'LTS'
]
print(f"LTS versions: {', '.join(lts_versions)}")

# Find versions supporting Java 17
java17_versions = [
    version_key
    for version_key, data in support_data['versions'].items()
    if '17' in data['supported_java_versions']
]
print(f"Java 17 compatible: {', '.join(java17_versions)}")
```

### Java

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class MuleRuntimeSupport {
    
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> supportData = mapper.readValue(
            new File("mule-runtime-support-matrix.json"), 
            Map.class
        );
        
        Map<String, Map<String, Object>> versions = 
            (Map<String, Map<String, Object>>) supportData.get("versions");
        
        // Get support dates for a specific version
        Map<String, Object> version49 = versions.get("4.9");
        System.out.println("Mule " + version49.get("type") + " 4.9");
        System.out.println("Standard Support ends: " + version49.get("end_of_standard_support"));
        System.out.println("Supported Java versions: " + version49.get("supported_java_versions"));
        
        // Check if a version is currently supported
        System.out.println("Is 4.6 still supported? " + isSupported(versions, "4.6"));
        
        // Find all LTS versions
        List<String> ltsVersions = versions.entrySet().stream()
            .filter(e -> "LTS".equals(e.getValue().get("type")))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        System.out.println("LTS versions: " + String.join(", ", ltsVersions));
        
        // Find versions supporting Java 17
        List<String> java17Versions = versions.entrySet().stream()
            .filter(e -> {
                List<String> javaVersions = (List<String>) e.getValue().get("supported_java_versions");
                return javaVersions.contains("17");
            })
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        System.out.println("Java 17 compatible: " + String.join(", ", java17Versions));
    }
    
    private static boolean isSupported(Map<String, Map<String, Object>> versions, String versionKey) {
        Map<String, Object> version = versions.get(versionKey);
        String endDate = (String) version.get("end_of_extended_support");
        return LocalDate.now().isBefore(LocalDate.parse(endDate)) || 
               LocalDate.now().isEqual(LocalDate.parse(endDate));
    }
}
```

### Groovy (for Mule Build Scripts)

```groovy
import groovy.json.JsonSlurper
import java.time.LocalDate

def supportDataFile = new File('mule-runtime-support-matrix.json')
def supportData = new JsonSlurper().parse(supportDataFile)

// Get runtime version from project property
def runtimeVersion = project.properties['mule.version'].tokenize('.')[0..1].join('.')

// Check support status
def versionData = supportData.versions[runtimeVersion]
def today = LocalDate.now()
def endOfSupport = LocalDate.parse(versionData.end_of_extended_support)

if (today.isAfter(endOfSupport)) {
    logger.warn("WARNING: Mule Runtime ${runtimeVersion} is past End of Extended Support!")
}

// Validate Java version
def currentJavaVersion = System.getProperty('java.version').tokenize('.')[0]
if (!versionData.supported_java_versions.contains(currentJavaVersion)) {
    throw new IllegalStateException(
        "Java ${currentJavaVersion} is not supported for Mule ${runtimeVersion}. " +
        "Supported versions: ${versionData.supported_java_versions.join(', ')}"
    )
}

logger.info("Mule ${runtimeVersion} (${versionData.type}) - Support until: ${versionData.end_of_extended_support}")
```

## Common Queries

### Q: Which versions are currently under Standard Support?

```javascript
const today = new Date().toISOString().split('T')[0];
const standardSupportVersions = Object.entries(supportData.versions)
  .filter(([_, data]) => today <= data.end_of_standard_support)
  .map(([version, data]) => ({ version, type: data.type, ends: data.end_of_standard_support }))
  .sort((a, b) => b.ends.localeCompare(a.ends));
```

### Q: What's the latest LTS version?

```javascript
const latestLTS = Object.entries(supportData.versions)
  .filter(([_, data]) => data.type === 'LTS')
  .sort(([_, a], [__, b]) => b.release_date.localeCompare(a.release_date))[0];
console.log(`Latest LTS: ${latestLTS[0]} (released ${latestLTS[1].release_date})`);
```

### Q: Which Java versions can I use with a specific runtime?

```javascript
const runtime = '4.8';
const javaVersions = supportData.versions[runtime].supported_java_versions;
console.log(`Mule ${runtime} supports Java: ${javaVersions.join(', ')}`);
```

### Q: When should I plan to upgrade from my current runtime?

```javascript
function getUpgradeWarning(versionKey) {
  const version = supportData.versions[versionKey];
  const today = new Date();
  const endOfStandardSupport = new Date(version.end_of_standard_support);
  const endOfExtendedSupport = new Date(version.end_of_extended_support);
  const endOfLife = new Date(version.end_of_life);
  
  if (today > endOfLife) {
    return `⛔ CRITICAL: ${versionKey} reached End of Life. Upgrade immediately!`;
  } else if (today > endOfExtendedSupport) {
    return `⚠️  WARNING: ${versionKey} is in End of Life Support period. Plan upgrade now.`;
  } else if (today > endOfStandardSupport) {
    return `⚠️  NOTICE: ${versionKey} is in Extended Support. Consider upgrade planning.`;
  } else {
    const daysUntilEnd = Math.floor((endOfStandardSupport - today) / (1000 * 60 * 60 * 24));
    return `✅ ${versionKey} is in Standard Support (${daysUntilEnd} days remaining)`;
  }
}
```

### Q: Find all versions supporting both Java 8 and 11

```javascript
const java8And11Versions = Object.entries(supportData.versions)
  .filter(([_, data]) => 
    data.supported_java_versions.includes('8') && 
    data.supported_java_versions.includes('11')
  )
  .map(([version, _]) => version);
```

## Integration Tips

### CI/CD Pipelines

Add a validation step to check runtime version support:

```yaml
# .github/workflows/validate-runtime.yml
- name: Check Runtime Support Status
  run: |
    node -e "
    const data = require('./mule-runtime-support-matrix.json');
    const version = process.env.MULE_VERSION;
    const info = data.versions[version];
    const today = new Date().toISOString().split('T')[0];
    
    if (today > info.end_of_extended_support) {
      console.error('ERROR: Runtime version is no longer supported');
      process.exit(1);
    }
    
    if (today > info.end_of_standard_support) {
      console.warn('WARNING: Runtime is in Extended Support only');
    }
    "
```

### Maven Build Validation

Add to your `pom.xml` or build script:

```xml
<plugin>
  <groupId>org.codehaus.gmaven</groupId>
  <artifactId>groovy-maven-plugin</artifactId>
  <executions>
    <execution>
      <phase>validate</phase>
      <goals>
        <goal>execute</goal>
      </goals>
      <configuration>
        <source>
          ${project.basedir}/validate-runtime-support.groovy
        </source>
      </configuration>
    </execution>
  </executions>
</plugin>
```

## Version History

- **2026-04-16**: Initial version with data from MuleSoft Product Versioning and Back Support Policy (pages 5-6)

## Maintenance

This file should be updated when:
- New Mule Runtime versions are released
- Support end dates are modified by MuleSoft
- Java version support changes

Always verify against official MuleSoft documentation before making production decisions.

## License

Data extracted from official MuleSoft documentation. Refer to MuleSoft's terms of service for usage rights.
