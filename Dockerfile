# -------- BUILD STAGE --------
FROM eclipse-temurin:26-jdk AS builder
WORKDIR /build

# Copy source
COPY . .

# Build the jar
RUN ./mvnw clean package -DskipTests

# -------- RUNTIME STAGE --------
FROM eclipse-temurin:26-jre
WORKDIR /app

# Copy jar from builder stage
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 7772

ENTRYPOINT ["java", "-XX:+UseZGC", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
