## [v1.7.0] - 2026-09-29

### 🚀 Features

- Rebundle Yoko into a single OSGi bundle

### 🐛 Bug Fixes

- Handle unexpected exceptions in client request interceptors
- Add ValueMemberFieldDescriptor for FVD fields without local equivalents
- Skip adding undefined serialPersistentFields to deserialization map
- Align enum classHash calculation with FVD structure
- Enforce COMPLETED_NO status for exceptions in send_request
- Implement CORBA spec completion status semantics for receive_exception
- Correct completion status for receive_reply exceptions
- Return empty array instead of throwing exception in ArgumentStrategyNull
- Handle SystemException in receive_request_service_contexts
- Update legacy activator bundle references
- Remove erroneous ORB singleton create_output_stream call

### ⚡ Performance

- Optimize LazyInitializedField to avoid unnecessary Waiter allocation
- Convert method invocation from reflection to MethodHandle
- Convert custom serialization to MethodHandle
- Convert writeObject to MethodHandle

## [v1.6.2] - 2026-07-23

A minor update containing only logging changes

## [v1.6.1] - 2026-04-15

### 🐛 Bug Fixes

- Decode Latin chars in corbaname URIs
- Incorrect policy type value

## [v1.6.0] - 2026-02-25

### 🚀 Features

- Use new UTF8-compatible (w)char codecs
- Use UTF-8 and UTF-16 codeset defaults

### 🐛 Bug Fixes

- Set input stream codecs in preUnmarshal()
- Use CESU-8 encoding with older Yoko
- Use Latin-1 charset to encode op name

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
