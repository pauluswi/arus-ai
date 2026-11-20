# ARUS AI Angular UI

Start the Spring Boot API from the repository root with `./mvnw spring-boot:run`, then run:

```shell
npm install
npm start
```

Open `http://localhost:4200`. The Angular development proxy forwards API calls to
`http://localhost:8080`. Local sign-in uses the API's development-only mock identity headers and is
not production authentication. No Docker or external service is required.

Build and test:

```shell
npm run build
npm test -- --watch=false
```
