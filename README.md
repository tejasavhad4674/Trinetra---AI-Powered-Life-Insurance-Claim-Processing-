# Trinetra - AI-Powered Life Insurance Claim Processing System

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Java](https://img.shields.io/badge/Java-17-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)
![React](https://img.shields.io/badge/React-18-blue.svg)
![TypeScript](https://img.shields.io/badge/TypeScript-5.x-blue.svg)

An intelligent, automated life insurance claim verification and processing platform that leverages **Azure OpenAI**, **Azure Computer Vision**, and **LangChain4j** to streamline claim submissions and reduce processing time from weeks to minutes.

---

## 🎯 Key Features

- **🤖 AI-Driven Decision Making**: Uses GPT-4o with LangChain4j agents to analyze claims and make approval/rejection decisions
- **📄 OCR Document Verification**: Extracts and validates data from death certificates, medical reports, and claim forms using Azure Computer Vision
- **🧠 RAG-Based Policy Rules**: Retrieves relevant policy constraints and guidelines using vector embeddings for accurate claim assessment
- **🔍 Multi-Document Analysis**: Processes multiple document types simultaneously for comprehensive fraud detection
- **⚡ Real-time Processing**: Submit claims and receive decisions in real-time with full AI reasoning transparency
- **☁️ Secure Cloud Storage**: Documents stored in Azure Blob Storage with secure access controls
- **🌐 Web Search Integration**: Optional Google Custom Search API for external verification

---

## 🛠️ Tech Stack

### Frontend
- **React 18** with TypeScript
- **Vite** for fast development
- **Tailwind CSS** for modern UI
- **Lucide React** icons

### Backend
- **Java 17** with Spring Boot 3.x
- **LangChain4j** for AI orchestration
- **Azure OpenAI** (GPT-4o, text-embedding-ada-002)
- **Azure Computer Vision** for OCR
- **Azure SQL Database**
- **Azure Blob Storage**
- **Hibernate/JPA** for data persistence

### AI/ML Components
- **RAG (Retrieval-Augmented Generation)** for policy knowledge base
- **Vector Embeddings** for semantic search
- **Multi-Agent System** with specialized tools (PolicyTool, PolicyRulesRagTool, WebSearchTool)
- **Google Custom Search API** integration (optional)

---

## 📋 Prerequisites

- **Java 17+**
- **Node.js 20+**
- **Maven 3.9+**
- **Azure Subscription** (for OpenAI, Computer Vision, SQL Database, Blob Storage)
- **Google Custom Search API Key** (optional, for web search)

---

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/tejasavhad4674/Trinetra---AI-Powered-Life-Insurance-Claim-Processing-.git
cd Trinetra---AI-Powered-Life-Insurance-Claim-Processing-
```

### 2. Backend Setup

#### Configure Azure Services

Edit `Claim Processor/src/main/resources/application.properties`:

```properties
# Azure SQL Database
spring.datasource.url=jdbc:sqlserver://YOUR_SERVER.database.windows.net:1433;database=YOUR_DB;encrypt=true;
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD

# Azure Blob Storage
azure.storage.connection-string=YOUR_STORAGE_CONNECTION_STRING
azure.storage.container-name=claims-documents

# Azure OpenAI
azure.openai.endpoint=https://YOUR_OPENAI_RESOURCE.cognitiveservices.azure.com/
azure.openai.key=YOUR_OPENAI_KEY
azure.openai.deployment=gpt-4o

# Azure OpenAI Embeddings (for RAG)
azure.openai.embedding.endpoint=https://YOUR_OPENAI_RESOURCE.cognitiveservices.azure.com/
azure.openai.embedding.key=YOUR_OPENAI_KEY
azure.openai.embedding.deployment=text-embedding-ada-002

# Azure Computer Vision
azure.vision.endpoint=https://YOUR_VISION_RESOURCE.cognitiveservices.azure.com/
azure.vision.key=YOUR_VISION_KEY

# Google Custom Search (Optional)
google.search.api.key=YOUR_GOOGLE_API_KEY
google.search.engine.id=YOUR_SEARCH_ENGINE_ID
```

#### Run the Backend

```bash
cd "Claim Processor"
mvn clean install
mvn spring-boot:run
```

Backend will start on `http://localhost:8080`

### 3. Frontend Setup

#### Install Dependencies

```bash
cd React
npm install
```

#### Configure Environment Variables

The project includes `.env.development` and `.env.production` files:

**`.env.development`** (for local development):
```
VITE_API_URL=http://localhost:8080
```

**`.env.production`** (for production build):
```
VITE_API_URL=https://your-backend-url.com
```

#### Run the Frontend

```bash
npm run dev
```

Frontend will start on `http://localhost:5173`

---

## 📱 Usage

1. **Open the Application**: Navigate to `http://localhost:5173` in your browser
2. **Fill Claim Form**: Enter policy number and deceased/nominee information
3. **Upload Documents**: 
   - Claim Form (required)
   - Death Certificate (required)
   - Doctor Report (required)
   - Police Report (required for accidents/suicide)
4. **Submit**: Click "Submit Claim"
5. **Review Decision**: AI will analyze documents and provide instant decision with reasoning

### Test Data

Use the following policy numbers for testing:
- **POL4004** - Active policy (Tejas Avhad)
- Other policies can be found in your Azure SQL Database

---

## 📂 Project Structure

```
├── Claim Processor/          # Spring Boot Backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/tejas/metlife/claimprocessor/
│   │   │   │       ├── config/           # CORS, Jackson config
│   │   │   │       ├── controller/       # REST controllers
│   │   │   │       ├── dto/              # Data transfer objects
│   │   │   │       ├── model/            # JPA entities
│   │   │   │       ├── repository/       # Data repositories
│   │   │   │       └── service/          # Business logic & AI services
│   │   │   │           ├── agent/        # LangChain4j AI agents
│   │   │   │           └── tool/         # AI tools
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   └── pom.xml
│
├── React/                     # React Frontend
│   ├── src/
│   │   ├── App.tsx           # Main application component
│   │   ├── main.tsx          # Entry point
│   │   └── index.css         # Global styles
│   ├── .env.development      # Dev environment variables
│   ├── .env.production       # Prod environment variables
│   └── package.json
│
└── Documents/                 # Sample test documents
```

---

## 🤖 AI Architecture

### Multi-Agent System

The system uses LangChain4j to orchestrate multiple AI agents:

1. **Claim Agent**: Main decision-making agent using GPT-4o
2. **Policy Tool**: Fetches policy details from database
3. **Policy Rules RAG Tool**: Retrieves relevant policy rules using vector embeddings
4. **Web Search Tool**: Optional external verification via Google Search

### RAG (Retrieval-Augmented Generation)

- Policy rules stored in **in-memory vector store**
- Uses **text-embedding-ada-002** for embeddings
- Enables semantic search for relevant policy constraints

### Document Processing Flow

```
Upload Documents → Azure Blob Storage → Azure Computer Vision (OCR) 
→ Extract Text → AI Agent Analysis → Decision (Approved/Rejected/Manual Review)
```

---

## 🌐 Deployment

### Azure Container Apps (Recommended)

The application can be deployed to Azure Container Apps. Update `.env.production` with your Azure backend URL:

```
VITE_API_URL=https://your-backend.azurecontainerapps.io
```

Build and deploy:
```bash
npm run build
# Deploy the dist/ folder to Azure
```

---

## 📊 API Endpoints

### POST `/api/claim/submit`

Submit a new insurance claim.

**Request:**
- Content-Type: `multipart/form-data`

**Form Fields:**
- `policyNumber` (string, required)
- `causeOfDeath` (string, required)
- `deceasedFullName` (string, required)
- `deceasedEmail` (string, required)
- `deceasedMobile` (string, required)
- `deceasedAddress` (string, required)
- `nomineeFullName` (string, required)
- `nomineeRelationship` (string, required)
- `nomineeMobile` (string, required)
- `claimForm` (file, required)
- `deathCertificate` (file, required)
- `doctorReport` (file, required)
- `policeReport` (file, conditional)

**Response:**
```json
{
  "status": "APPROVED | REJECTED | MANUAL_REVIEW",
  "message": "Detailed reasoning",
  "claimReference": "CLM-12345678"
}
```

---

## 🔒 Security

- Environment variables for sensitive credentials
- CORS configured for specific origins
- Azure SQL Database with encrypted connections
- Azure Blob Storage with secure access controls
- Secure API key management for Azure services

---

## 📝 Documentation

Additional documentation available in the project:
- [AI Services Explanation](Claim%20Processor/AI-SERVICES-EXPLANATION.md)
- [RAG Integration](Claim%20Processor/RAG_INTEGRATION.md)
- [Multi-Document Verification](Claim%20Processor/MULTI_DOCUMENT_VERIFICATION.md)
- [Project Structure](Claim%20Processor/PROJECT-STRUCTURE.md)
- [Request Flow Diagram](Claim%20Processor/REQUEST-FLOW-DIAGRAM.md)

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## 👤 Author

**Tejas Avhad**
- GitHub: [@tejasavhad4674](https://github.com/tejasavhad4674)

---

## 🙏 Acknowledgments

- **Azure OpenAI** for GPT-4o and embeddings
- **LangChain4j** for AI orchestration framework
- **Azure Computer Vision** for OCR capabilities
- **Spring Boot** for robust backend framework
- **React** for modern frontend development

---

*Trinetra (त्रिनेत्र) - Sanskrit for "three eyes" - symbolizing comprehensive vision through technology, intelligence, and insight.*
