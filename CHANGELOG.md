## [v1.5.3] - 2026-01-09

### 🚀 Features

- Remove euro-centric codeset locales; prefer UTF-8
- Default native char codeset to UTF-8
- Add comprehensive release system with git-cliff integration

### 🐛 Bug Fixes

- Garbled GIOP message logging
- Skip bounds check for UTF-8/UTF-16/UCS-2
- Correctly identify UTF-8 2-byte and 3-byte sequences
## [v1.5.2] - 2025-11-18

### 🐛 Bug Fixes

- Handle SystemException from createLocateRequestDowncall
## [v1.5.1]

### Bug Fixes
- Make bundle version OSGi compliant

## [v1.5.0]

### 🚀 Features
- Logging improvements using new `yoko.verbose.*` loggers
- Detect IBM Java ORB and marshal `java.util.Date` sympathetically

### 🐛 Bug Fixes
- Set SO_REUSEADDR from DefaultConnectionHelper
- Improve little endian handling in ReadBuffer
- Send service contexts on first GIOP 1.2 message
- Support java transaction exceptions if present
- Use local codebase for collocated invocations
- Make CodeBaseProxy.getCodeBase() public
- Stop infinite recursion in TypeCode.toString()
- Remove unnecessary throws declarations
- Marshal non-serializable fields as abstract values
- Don't nest INTERNAL exceptions needlessly
- Avoid processing fields for non-serializable classes
- Always setReuseAddress(true) on server sockets
- Unmarshal String in Comparable field
