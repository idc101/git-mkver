FROM ghcr.io/graalvm/graalvm-ce:latest

RUN gu install native-image

# RUN rm /bin/sh && ln -s /bin/bash /bin/sh
# RUN yum -qq -y install curl
# RUN curl -s https://get.sdkman.io | bash
# RUN chmod a+x "$HOME/.sdkman/bin/sdkman-init.sh"
# RUN source "$HOME/.sdkman/bin/sdkman-init.sh"
#
# RUN sdk install sbt
