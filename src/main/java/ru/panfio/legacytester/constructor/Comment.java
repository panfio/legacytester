package ru.panfio.legacytester.constructor;

public enum Comment {
    GIVEN("        //Given\n"),
    WHEN("        //When\n"),
    THEN("        //Then\n");

    private String text;

    Comment(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }

    public String text(int spaceBefore) {
        return new String(new char[spaceBefore]).replace("\0", " ");
    }

}
