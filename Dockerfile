FROM amazoncorretto:8-alpine3.17-jre

RUN apk update && apk add bash && apk add nodejs npm && npm install -g csslint@1.0.5
