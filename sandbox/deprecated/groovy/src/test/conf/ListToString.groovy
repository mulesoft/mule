class ListToString {
  getString(src) {
    result="";
    src.each { t | result+= " " + t };
    return result.substring(1);
  }
}