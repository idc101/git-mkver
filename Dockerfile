FROM rust:1-bookworm

RUN apt-get update && apt-get -y install git zip unzip openssl

WORKDIR /workspace
CMD ["./build-linux.sh"]
