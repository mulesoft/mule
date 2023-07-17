if [ "$1" = "java8" ]; then
  EXPR='s&src/main/java11"&src/main/java"&'
elif [ "$1" = "java11" ]; then
  EXPR='s&src/main/java"&src/main/java11"&'
fi

sed "$EXPR" modules/jar-handling-utils/mule-jar-handling-utils.iml | uniq > temp && mv temp modules/jar-handling-utils/mule-jar-handling-utils.iml
sed "$EXPR" modules/jpms-utils/mule-module-jpms-utils.iml | uniq > temp && mv temp modules/jpms-utils/mule-module-jpms-utils.iml
sed "$EXPR" modules/service-artifact-impl/mule-module-service-artifact-impl.iml | uniq > temp && mv temp modules/service-artifact-impl/mule-module-service-artifact-impl.iml
