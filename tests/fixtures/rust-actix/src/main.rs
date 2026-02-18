use actix_web::{web, App, HttpServer, HttpResponse, Responder};
use serde::Serialize;

#[derive(Serialize)]
struct HealthResponse {
    status: String,
    service: String,
}

#[derive(Serialize)]
struct InfoResponse {
    name: String,
    version: String,
    language: String,
    framework: String,
}

#[derive(Serialize)]
struct Item {
    id: u32,
    name: String,
}

async fn health() -> impl Responder {
    HttpResponse::Ok().json(HealthResponse {
        status: "UP".to_string(),
        service: "rust-actix-sample".to_string(),
    })
}

async fn info() -> impl Responder {
    HttpResponse::Ok().json(InfoResponse {
        name: "DevSecOps Rust Sample".to_string(),
        version: "1.0.0".to_string(),
        language: "rust".to_string(),
        framework: "actix-web".to_string(),
    })
}

async fn items() -> impl Responder {
    HttpResponse::Ok().json(vec![
        Item { id: 1, name: "Item A".to_string() },
        Item { id: 2, name: "Item B".to_string() },
    ])
}

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    HttpServer::new(|| {
        App::new()
            .route("/api/health", web::get().to(health))
            .route("/api/info", web::get().to(info))
            .route("/api/items", web::get().to(items))
    })
    .bind("0.0.0.0:8080")?
    .run()
    .await
}

#[cfg(test)]
mod tests {
    use super::*;
    use actix_web::test;

    #[actix_rt::test]
    async fn test_health() {
        let app = test::init_service(
            App::new().route("/api/health", web::get().to(health))
        ).await;
        let req = test::TestRequest::get().uri("/api/health").to_request();
        let resp = test::call_service(&app, req).await;
        assert!(resp.status().is_success());
    }

    #[actix_rt::test]
    async fn test_info() {
        let app = test::init_service(
            App::new().route("/api/info", web::get().to(info))
        ).await;
        let req = test::TestRequest::get().uri("/api/info").to_request();
        let resp = test::call_service(&app, req).await;
        assert!(resp.status().is_success());
    }
}
