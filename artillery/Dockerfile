FROM nunopreguica/scc2122-test
WORKDIR /usr/src/app
COPY stress.yml /usr/src/app
COPY create-channels.yml /usr/src/app
COPY create-messages.yml /usr/src/app
COPY create-users.yml /usr/src/app
COPY test-utils.js /usr/src/app
COPY images /usr/src/app/images
COPY entrypoint.sh /

RUN apk add --no-cache openssh
RUN apk add --no-cache openrc
RUN echo 'PasswordAuthentication yes' >> /etc/ssh/sshd_config
RUN adduser -h /home/noob -s /bin/sh -D noob
RUN echo -n 'noob:password' | chpasswd
EXPOSE 22
ENTRYPOINT ["/entrypoint.sh"]
