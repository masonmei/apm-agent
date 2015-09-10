# Mac 安装thrift 0.9.0过程
#
# 1. install macport
#    http://www.macports.org/install.php 下载专用版本的macports
# 2. sudo port install thrift
# 3. uninstall the thrift and install the 0.9.0 version
#    sudo port uninstall thrift
# 4. 下载 0.9.0 版本的源代码进行编译安装
#    curl -OL http://archive.apache.org/dist/thrift/0.9.0/thrift-0.9.0.tar.gz
#    tar zxf thrift-0.9.0.tar.gz
#    cd thrift-0.9.0
#    ./configure --prefix=/Users/mason/Applications/thrift --with-ruby=no --with-php=no --with-python=no --with-cpp=no --with-java=no --with-c_glib=no
#    make && make install
#    将下面内容加入到profile中
#     export THRIFT_HOME=/Users/mason/Applications/thrift
#     export PATH=$THRIFT_HOME/bin:$PATH
#
#    * 说明可以更新第4步中prefix配置来修改安装路径
#

mvn generate-sources -P for-mac -DskipTests
rc=$?
if [[ $rc != 0 ]] ; then
        echo "BUILD FAILED $rc"
        exit $rc
fi