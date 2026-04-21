param([string]$tag)

if (-not $tag) {
    Write-Host "Nhập tag"
    exit
}

docker build -t truongikpk/bookstore-order-service:$tag .
docker push truongikpk/bookstore-order-service:$tag

# .\push.ps1 v1.0.0
# ./mvnw compile