# API Examples & Testing Guide

## Using curl

### 1. Upload a Document

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@HR_Policy.pdf" \
  -H "Accept: application/json"
```

**Response:**
```json
{
  "success": true,
  "message": "Document uploaded and processed successfully",
  "data": {
    "id": 1,
    "fileName": "HR_Policy.pdf",
    "fileType": "application/pdf",
    "fileSize": 245760,
    "totalChunks": 45,
    "uploadedAt": "2024-02-07T10:30:00"
  }
}
```

### 2. Ask a Question (RAG)

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What is the sick leave policy?",
    "topK": 3
  }'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "question": "What is the sick leave policy?",
    "answer": "According to the employee handbook, employees are entitled to 12 paid sick days per year. Sick leave can be used for personal illness or to care for immediate family members. Unused sick days can be carried over to the next year with a maximum accumulation of 30 days.",
    "sources": [
      {
        "documentId": 1,
        "documentName": "HR_Policy.pdf",
        "chunkIndex": 5,
        "content": "Employees receive 12 paid sick days annually. These days can be used for personal illness or to care for immediate family members...",
        "relevanceScore": 0.92
      },
      {
        "documentId": 1,
        "documentName": "HR_Policy.pdf",
        "chunkIndex": 6,
        "content": "Unused sick leave can be carried over to the following year, up to a maximum of 30 days...",
        "relevanceScore": 0.87
      }
    ],
    "totalSources": 2
  }
}
```

### 3. Semantic Search (Without AI Generation)

```bash
curl -X GET "http://localhost:8080/api/query/search?query=vacation%20policy&topK=5"
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "documentId": 1,
      "documentName": "HR_Policy.pdf",
      "chunkIndex": 10,
      "content": "Vacation time accrues at a rate of 1.25 days per month for a total of 15 days per year...",
      "relevanceScore": 0.88
    }
  ]
}
```

### 4. List All Documents

```bash
curl -X GET http://localhost:8080/api/documents
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "fileName": "HR_Policy.pdf",
      "fileSize": 245760,
      "totalChunks": 45,
      "uploadedAt": "2024-02-07T10:30:00"
    },
    {
      "id": 2,
      "fileName": "Tech_Documentation.pdf",
      "fileSize": 512000,
      "totalChunks": 87,
      "uploadedAt": "2024-02-07T11:15:00"
    }
  ]
}
```

### 5. Get Specific Document

```bash
curl -X GET http://localhost:8080/api/documents/1
```

### 6. Delete Document

```bash
curl -X DELETE http://localhost:8080/api/documents/1
```

**Response:**
```json
{
  "success": true,
  "message": "Document deleted successfully",
  "data": null
}
```

## Using Postman

### Collection Setup

1. **Create New Collection**: "Corporate Knowledge Base"

2. **Set Variables**:
   - `baseUrl`: `http://localhost:8080`
   - `apiBase`: `{{baseUrl}}/api`

### Request Examples

#### Upload Document
```
Method: POST
URL: {{apiBase}}/documents/upload
Body: form-data
  - Key: file
  - Type: File
  - Value: [Select your PDF]
```

#### Query with RAG
```
Method: POST
URL: {{apiBase}}/query
Headers:
  Content-Type: application/json
Body (raw JSON):
{
  "question": "What is the remote work policy?",
  "topK": 3
}
```

#### Semantic Search
```
Method: GET
URL: {{apiBase}}/query/search
Params:
  - query: vacation policy
  - topK: 5
```

## Using Python (requests)

```python
import requests
import json

BASE_URL = "http://localhost:8080/api"

# Upload document
def upload_document(file_path):
    with open(file_path, 'rb') as f:
        files = {'file': f}
        response = requests.post(f"{BASE_URL}/documents/upload", files=files)
    return response.json()

# Ask a question
def ask_question(question, top_k=3):
    data = {
        "question": question,
        "topK": top_k
    }
    response = requests.post(
        f"{BASE_URL}/query",
        headers={"Content-Type": "application/json"},
        data=json.dumps(data)
    )
    return response.json()

# Semantic search
def semantic_search(query, top_k=5):
    params = {"query": query, "topK": top_k}
    response = requests.get(f"{BASE_URL}/query/search", params=params)
    return response.json()

# List documents
def list_documents():
    response = requests.get(f"{BASE_URL}/documents")
    return response.json()

# Example usage
if __name__ == "__main__":
    # Upload
    result = upload_document("HR_Policy.pdf")
    print(f"Uploaded: {result['data']['fileName']}")
    
    # Query
    answer = ask_question("What is the sick leave policy?")
    print(f"Answer: {answer['data']['answer']}")
    
    # Search
    results = semantic_search("vacation days")
    print(f"Found {len(results['data'])} results")
```

## Using JavaScript (fetch)

```javascript
const BASE_URL = 'http://localhost:8080/api';

// Upload document
async function uploadDocument(file) {
  const formData = new FormData();
  formData.append('file', file);
  
  const response = await fetch(`${BASE_URL}/documents/upload`, {
    method: 'POST',
    body: formData
  });
  
  return await response.json();
}

// Ask a question
async function askQuestion(question, topK = 3) {
  const response = await fetch(`${BASE_URL}/query`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ question, topK })
  });
  
  return await response.json();
}

// Semantic search
async function semanticSearch(query, topK = 5) {
  const params = new URLSearchParams({ query, topK });
  const response = await fetch(`${BASE_URL}/query/search?${params}`);
  return await response.json();
}

// Example usage
(async () => {
  // Ask question
  const result = await askQuestion("What is the sick leave policy?");
  console.log('Answer:', result.data.answer);
  console.log('Sources:', result.data.sources.length);
})();
```

## Testing Scenarios

### Scenario 1: HR Policy Questions

```bash
# Upload HR policy
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@HR_Policy.pdf"

# Test questions
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"question": "How many vacation days do employees get?"}'

curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the parental leave policy?"}'

curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"question": "Can employees work remotely?"}'
```

### Scenario 2: Technical Documentation

```bash
# Upload technical docs
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@API_Documentation.pdf"

# Test questions
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"question": "How do I authenticate with the API?"}'

curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What are the rate limits?"}'
```

### Scenario 3: Multiple Documents

```bash
# Upload multiple documents
curl -X POST http://localhost:8080/api/documents/upload -F "file=@HR_Policy.pdf"
curl -X POST http://localhost:8080/api/documents/upload -F "file=@Benefits.pdf"
curl -X POST http://localhost:8080/api/documents/upload -F "file=@Code_of_Conduct.pdf"

# Ask cross-document question
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What employee benefits are mentioned?", "topK": 5}'
```

## Performance Testing

### Load Test with Apache Bench

```bash
# Test query endpoint
ab -n 100 -c 10 -T "application/json" \
  -p query.json \
  http://localhost:8080/api/query

# query.json content:
# {"question": "What is the policy?", "topK": 3}
```

### Measure Response Times

```bash
# Upload time
time curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@large_document.pdf"

# Query time
time curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the main topic?"}'
```

## Error Handling Examples

### Invalid File Type

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@document.txt"

# Response:
# {
#   "success": false,
#   "message": "Only PDF files are supported"
# }
```

### Empty Question

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"question": ""}'

# Response:
# {
#   "success": false,
#   "message": "Question cannot be empty"
# }
```

### Document Not Found

```bash
curl -X DELETE http://localhost:8080/api/documents/999

# Response:
# {
#   "success": false,
#   "message": "Document not found"
# }
```

## Integration Testing

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testUploadAndQuery() {
        // Upload document
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ClassPathResource("test.pdf"));
        
        ResponseEntity<ApiResponse> uploadResponse = 
            restTemplate.postForEntity("/api/documents/upload", body, ApiResponse.class);
        
        assertTrue(uploadResponse.getBody().isSuccess());
        
        // Query document
        QueryRequest queryRequest = new QueryRequest("What is this about?", 3);
        ResponseEntity<ApiResponse> queryResponse = 
            restTemplate.postForEntity("/api/query", queryRequest, ApiResponse.class);
        
        assertTrue(queryResponse.getBody().isSuccess());
    }
}
```

---

**Pro Tip**: Use the included React frontend at http://localhost:8080 for interactive testing!
