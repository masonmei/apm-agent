mvn generate-sources -P with-thrift -Dmaven.test.skip -Dthrift.executable.path=./src/compiler/linux/thrift-0.9.2

rc=$?
if [[ $rc != 0 ]] ; then
        echo "BUILD FAILED $rc"
        exit $rc
fi