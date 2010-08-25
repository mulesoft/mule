def result = ""
payload.each { result += " $it" }
return result.substring(1) // cut the leading space
