package main

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

func setupRouter() *gin.Engine {
	r := gin.Default()

	r.GET("/api/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{
			"status":  "UP",
			"service": "golang-gin-sample",
		})
	})

	r.GET("/api/info", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{
			"name":      "DevSecOps Go Sample",
			"version":   "1.0.0",
			"language":  "golang",
			"framework": "gin",
		})
	})

	r.GET("/api/items", func(c *gin.Context) {
		c.JSON(http.StatusOK, []gin.H{
			{"id": 1, "name": "Item A"},
			{"id": 2, "name": "Item B"},
		})
	})

	return r
}

func main() {
	r := setupRouter()
	r.Run(":8080")
}
