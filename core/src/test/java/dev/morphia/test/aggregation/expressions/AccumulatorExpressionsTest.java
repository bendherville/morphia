package dev.morphia.test.aggregation.expressions;

import java.time.LocalDate;
import java.util.List;

import dev.morphia.aggregation.stages.AddFields;
import dev.morphia.aggregation.stages.Group;
import dev.morphia.aggregation.stages.Sort;
import dev.morphia.test.models.User;

import org.bson.Document;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.avg;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.function;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.last;
import static dev.morphia.aggregation.expressions.AccumulatorExpressions.push;
import static dev.morphia.aggregation.expressions.DateExpressions.dayOfYear;
import static dev.morphia.aggregation.expressions.DateExpressions.year;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.aggregation.expressions.MathExpressions.multiply;
import static dev.morphia.aggregation.expressions.WindowExpressions.stdDevSamp;
import static dev.morphia.aggregation.stages.Group.id;
import static java.util.List.of;
import static org.bson.Document.parse;

public class AccumulatorExpressionsTest extends ExpressionsTestBase {

    @Test
    public void testFunction() {
        insert("players", of(
                parse("{ _id: 1, name: 'Miss Cheevous',  scores: [ 10, 5, 10 ] }"),
                parse("{ _id: 2, name: 'Miss Ann Thrope', scores: [ 10, 10, 10 ] }"),
                parse("{ _id: 3, name: 'Mrs. Eppie Delta ', scores: [ 9, 8, 8 ] }")));

        List<Document> actual = getDs().aggregate("players")
                .addFields(AddFields.addFields()
                        .field("isFound", function("function(name) {\n"
                                + "  return hex_md5(name) == "
                                + "\"15b0a220baa16331e8d80e15367677ad\"\n"
                                + "}", field("name")))
                        .field("message", function("function(name, scores) {\n"
                                + "  let total = Array.sum(scores);\n"
                                +
                                "  return `Hello ${name}.  Your total score is"
                                + " ${total}.`\n"
                                + "}", field("name"), field("scores"))))
                .execute(Document.class)
                .toList();

        assertDocumentEquals(actual, of(
                parse("{ '_id' : 1, 'name' : 'Miss Cheevous', 'scores' : [ 10, 5, 10 ], 'isFound' : false, 'message' : 'Hello Miss Cheevous. "
                        + " Your total score is 25.' }"),
                parse("{ '_id' : 2, 'name' : 'Miss Ann Thrope', 'scores' : [ 10, 10, 10 ], 'isFound' : true, 'message' : 'Hello Miss Ann "
                        + "Thrope.  Your total score is 30.' }"),
                parse("{ '_id' : 3, 'name' : 'Mrs. Eppie Delta ', 'scores' : [ 9, 8, 8 ], 'isFound' : false, 'message' : 'Hello Mrs. Eppie "
                        + "Delta .  Your total score is 25.' }")));
    }

    @Test
    public void testLast() {
        largerDataSet();

        List<Document> actual = getDs().aggregate("sales")
                .sort(Sort.sort()
                        .ascending("item", "date"))
                .group(Group.group(id("item"))
                        .field("lastSalesDate", last(field("date"))))
                .execute(Document.class)
                .toList();

        assertDocumentEquals(actual, of(
                parse("{ '_id' : 'xyz', 'lastSalesDate' : ISODate('2014-02-15T14:12:12Z') }"),
                parse("{ '_id' : 'jkl', 'lastSalesDate' : ISODate('2014-02-03T09:00:00Z') }"),
                parse("{ '_id' : 'abc', 'lastSalesDate' : ISODate('2014-02-15T08:00:00Z') }")));
    }

    @Test
    public void testMax() {
        regularDataSet();

        List<Document> actual = getDs().aggregate("sales")
                .group(Group.group(id("item"))
                        .field("avgAmount", avg(multiply(
                                field("price"), field("quantity"))))
                        .field("avgQuantity", avg(field("quantity"))))
                .execute(Document.class)
                .toList();

        assertDocumentEquals(actual, of(
                parse("{'_id' : 'jkl', 'avgAmount' : 20.0, 'avgQuantity' : 1.0 }"),
                parse("{'_id' : 'abc', 'avgAmount' : 60.0, 'avgQuantity' : 6.0 }"),
                parse("{'_id' : 'xyz', 'avgAmount' : 37.5, 'avgQuantity' : 7.5 }")));
    }

    @Test
    public void testPush() {
        largerDataSet();

        List<Document> actual = getDs().aggregate("sales")
                .group(Group.group(id()
                        .field("day", dayOfYear(field("date")))
                        .field("year", year(field("date"))))
                        .field("itemsSold", push()
                                .field("item", field("item"))
                                .field("quantity", field("quantity"))))
                .execute(Document.class)
                .toList();

        assertDocumentEquals(actual, of(
                parse("{ '_id' : { 'day' : 46, 'year' : 2014 },'itemsSold' : [{ 'item' : 'abc', 'quantity' : 10 }, { 'item' : 'xyz', "
                        + "'quantity' : 10 },{ 'item' : 'xyz', 'quantity' : 5 },{ 'item' : 'xyz', 'quantity' : 10 }]}"),
                parse("{ '_id' : { 'day' : 34, 'year' : 2014 },'itemsSold' : [{ 'item' : 'jkl', 'quantity' : 1 },{ 'item' : 'xyz', "
                        + "'quantity' : 5 }]}"),
                parse("{ '_id' : { 'day' : 1, 'year' : 2014 },'itemsSold' : [ { 'item' : 'abc', 'quantity' : 2 } ]}")));
    }

    @Test
    public void testStdDevSamp() {
        // we don't have a data set to test numbers so let's at least test we're creating the correct structures for the server
        getDs().save(new User("", LocalDate.now()));
        getDs().aggregate(User.class)
                .sample(100)
                .group(Group.group()
                        .field("ageStdDev", stdDevSamp(field("age"))))
                .execute(Document.class)
                .toList();
    }

    private void largerDataSet() {
        insert("sales", of(
                parse("{ '_id' : 1, 'item' : 'abc', 'price' : 10, 'quantity' : 2, 'date' : ISODate('2014-01-01T08:00:00Z') }"),
                parse("{ '_id' : 2, 'item' : 'jkl', 'price' : 20, 'quantity' : 1, 'date' : ISODate('2014-02-03T09:00:00Z') }"),
                parse("{ '_id' : 3, 'item' : 'xyz', 'price' : 5, 'quantity' : 5, 'date' : ISODate('2014-02-03T09:05:00Z') }"),
                parse("{ '_id' : 4, 'item' : 'abc', 'price' : 10, 'quantity' : 10, 'date' : ISODate('2014-02-15T08:00:00Z') }"),
                parse("{ '_id' : 5, 'item' : 'xyz', 'price' : 5, 'quantity' : 10, 'date' : ISODate('2014-02-15T09:05:00Z') }"),
                parse("{ '_id' : 6, 'item' : 'xyz', 'price' : 5, 'quantity' : 5, 'date' : ISODate('2014-02-15T12:05:10Z') }"),
                parse("{ '_id' : 7, 'item' : 'xyz', 'price' : 5, 'quantity' : 10, 'date' : ISODate('2014-02-15T14:12:12Z') }")));
    }

    private void regularDataSet() {
        insert("sales", of(
                parse("{ '_id' : 1, 'item' : 'abc', 'price' : 10, 'quantity' : 2, 'date' : ISODate('2014-01-01T08:00:00Z') }"),
                parse("{ '_id' : 2, 'item' : 'jkl', 'price' : 20, 'quantity' : 1, 'date' : ISODate('2014-02-03T09:00:00Z') }"),
                parse("{ '_id' : 3, 'item' : 'xyz', 'price' : 5, 'quantity' : 5, 'date' : ISODate('2014-02-03T09:05:00Z') }"),
                parse("{ '_id' : 4, 'item' : 'abc', 'price' : 10, 'quantity' : 10, 'date' : ISODate('2014-02-15T08:00:00Z') }"),
                parse("{ '_id' : 5, 'item' : 'xyz', 'price' : 5, 'quantity' : 10, 'date' : ISODate('2014-02-15T09:12:00Z') }")));
    }
}
