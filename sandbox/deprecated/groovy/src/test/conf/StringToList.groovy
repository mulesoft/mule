class StringToList {
  transform(src) {
    return src.toString().tokenize(" ");
  }
}