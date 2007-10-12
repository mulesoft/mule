def result = ""
src.each { result += " $it" }
return result.substring(1) // cut the leading space