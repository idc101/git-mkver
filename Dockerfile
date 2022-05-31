FROM ubuntu:22.04

RUN apt-get update
RUN apt-get -y install curl zip unzip git gcc zlib1g-dev

RUN curl -s "https://get.sdkman.io" | bash
ENV SDKMAN_INIT="/root/.sdkman/bin/sdkman-init.sh"

SHELL ["/bin/bash", "-c"]

RUN source "$SDKMAN_INIT" && \
    sdk install java 22.1.0.r17-grl && \
    sdk install sbt && \
    gu install native-image

CMD source "$SDKMAN_INIT" && ./build.sh
