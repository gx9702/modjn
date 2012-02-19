package modbus;

import modbus.client.ModbusClientHandler;
import modbus.server.ModbusServerHandler;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;

/**
 *
 * @author ag
 */
public class ModbusPipelineFactory implements
        ChannelPipelineFactory {
    
    private static final ChannelHandler MODBUS_ENCODER = new ModbusEncoder();
    private static final ChannelHandler MODBUS_DECODER = new ModbusDecoder();
    
    private final boolean server;

    public ModbusPipelineFactory(boolean server) {
        this.server = server;
    }
    
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();

        /*
         * Modbus TCP Frame Description
         *  - max. 260 Byte (ADU = 7 Byte MBAP + 253 Byte PDU)
         *  - Length field includes Unit Identifier + PDU 
         * 
         * <----------------------------------------------- ADU -------------------------------------------------------->
         * <---------------------------- MBAP -----------------------------------------><------------- PDU ------------->
         * +------------------------+---------------------+----------+-----------------++---------------+---------------+
         * | Transaction Identifier | Protocol Identifier | Length   | Unit Identifier || Function Code | Data          |
         * | (2 Byte)               | (2 Byte)            | (2 Byte) | (1 Byte)        || (1 Byte)      | (1 - 252 Byte |
         * +------------------------+---------------------+----------+-----------------++---------------+---------------+
         */
        pipeline.addLast("framer", new LengthFieldBasedFrameDecoder(ModbusConstants.ADU_MAX_LENGTH, 4, 2));

        //Modbus encoder, decoder
        pipeline.addLast("encoder", MODBUS_ENCODER);
        pipeline.addLast("decoder", MODBUS_DECODER);

        // and then business logic.
        if(server) {
            pipeline.addLast("handler", new ModbusServerHandler());
        } else {    
            pipeline.addLast("handler", new ModbusClientHandler());
        }
        
        return pipeline;
    }
}