#!/bin/sh

set -e

#
# The IP Address will have a scaled instance
#
IP_ADDRESS=$(ifconfig eth0 | grep 'inet ' | awk '{print $2}')

#
# grab the last numerical field of the hostname and treat that as the unique ID of this scaled instance.
#
INSTANCE_ID=$(dig -x $IP_ADDRESS +short | sed -E "s/(.*)-([0-9]+)\.(.*)/\2/")

export GROUP_INSTANCE_ID=${INSTANCE_ID}
export CLIENT_ID=${CLIENT_ID_PREFIX:-}-${INSTANCE_ID}

cd /app
tar xfv /app.tar

PROJECT="$(ls -A)"
APPLICATION=$(echo "$PROJECT" | sed -E -e 's/(.*)-(.*)/\1/')
VERSION=$(echo "$PROJECT" | sed -E -e 's/(.*)-(.*)/\2/')

COMMAND="/app/${PROJECT}/bin/${APPLICATION}"

#
# use exec so signals are properly handled
#
exec "${COMMAND}" "$@"
