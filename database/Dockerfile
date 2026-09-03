FROM mysql:8.0

ENV MYSQL_DATABASE=payment_proof
ENV MYSQL_USER=payment_user
ENV MYSQL_PASSWORD=payment_password
ENV MYSQL_ROOT_PASSWORD=root_secure_password

# Copy initialization scripts
COPY schema.sql /docker-entrypoint-initdb.d/01-schema.sql
COPY seed.sql /docker-entrypoint-initdb.d/02-seed.sql

EXPOSE 3306
