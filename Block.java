/**
 * Created by Julian M. <julian@ddsn.org> on 22.02.14.
 */
public class Block {

    private Code code;

    public Block(Code code) {
        this.code = code;
    }

    public Code getCode() {
        return code;
    }

    @Override
    public String toString() {
        return "Block " + code;
    }

}
