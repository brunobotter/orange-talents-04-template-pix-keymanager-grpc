package br.com.bruno.orange.pixdesafio

import io.micronaut.runtime.Micronaut.*
fun main(args: Array<String>) {
	build()
	    .args(*args)
		.packages("br.com.bruno.orange.pixdesafio")
		.start()
}

