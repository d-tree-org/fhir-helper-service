run:
    docker-compose -f docker-compose.yml -f docker-compose.dev.yml --env-file .dev.env up -d

down:
    docker-compose  -f docker-compose.yml -f docker-compose.dev.yml down