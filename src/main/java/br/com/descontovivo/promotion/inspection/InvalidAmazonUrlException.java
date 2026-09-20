package br.com.descontovivo.promotion.inspection;

/** Link da Amazon que não pode ser gravado; o IllegalArgumentExceptionMapper responde 422 com a mensagem. */
public class InvalidAmazonUrlException extends IllegalArgumentException {
    public InvalidAmazonUrlException(String message) { super(message); }
}
