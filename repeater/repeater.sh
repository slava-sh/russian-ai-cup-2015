pushd `dirname $0` > /dev/null
SCRIPTDIR=`pwd`
popd > /dev/null

read -r token <./token.txt
java -cp ".:*:$SCRIPTDIR/*" -jar repeater.jar $token &
