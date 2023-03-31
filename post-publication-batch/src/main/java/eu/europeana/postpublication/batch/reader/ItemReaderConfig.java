package eu.europeana.postpublication.batch.reader;

import dev.morphia.query.experimental.filters.Filters;
import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.postpublication.batch.config.PostPublicationSettings;
import eu.europeana.postpublication.service.BatchRecordService;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class ItemReaderConfig {

    private final BatchRecordService batchRecordService;
    private final PostPublicationSettings postPublicationSettings;
    private final List<String> records = Arrays.asList("/925/Culturalia_7e67d601_fe38_45d1_b495_2fcb3938a997",
            "/925/Culturalia_3acd5895_d06d_4480_9bc6_643d36a4eb61",
            "/925/Culturalia_658f3cda_3a3f_4b0b_b8d9_b0474b2eb350",
            "/925/Culturalia_7f81d215_1a09_410d_a37e_c276f3e226f9",
            "/925/Culturalia_0563bb06_4fc4_40bb_a5e2_3b77526c8aec",
            "/925/Culturalia_be60f347_9118_4e16_8960_aeef9e66cd0e",
            "/925/Culturalia_aa7c09a4_8871_4bfd_afa5_1be28ed2c5a0",
            "/925/Culturalia_f2568055_e4a6_4c44_8905_8e616b879527",
            "/925/Culturalia_fc23b6d5_4504_459a_96b3_1e7e28c34807",
            "/925/Culturalia_ee6d6bc9_0b28_4df0_8686_76605e4ba3fd",
            "/925/Culturalia_41f92d39_0446_491b_94a5_b4f1a41b914b",
            "/925/Culturalia_7b2f80c1_2902_4e0f_8894_ed713a8b62a3",
            "/925/Culturalia_78476685_40e6_4650_8ccf_9454d63d4752",
            "/925/Culturalia_1b64d081_0afb_43d6_b0c6_c24c1842de4a",
            "/925/Culturalia_7f3eaa60_5265_4182_80ed_347edaa679d0",
            "/925/Culturalia_71245660_29bd_43c8_8cd7_3784540781d4",
            "/925/Culturalia_7d974f12_3fe6_4f54_822c_d03b12c7071e",
            "/925/Culturalia_e7696a49_a093_4f1a_ac3d_3fcf95ecf394",
            "/925/Culturalia_0883ed59_d2bf_4c62_a6e6_702a60d1f197",
            "/925/Culturalia_a247417b_01b6_409f_95a1_589a6245e7d4",
            "/925/Culturalia_7f792425_d260_44ac_a22c_6ff9e49dfe6a",
            "/925/Culturalia_be4431f9_3f54_4f83_b223_9bea074cf31c",
            "/925/Culturalia_11751860_8746_4183_9979_d8c880cc6eea",
            "/925/Culturalia_5c6f236d_5ae2_4b2c_9dee_a142e33213de",
            "/925/Culturalia_b0a789a6_3582_4d48_9956_7e0c2e31e258",
            "/925/Culturalia_550e3152_e003_4d8c_a196_8fcae15d7791",
            "/925/Culturalia_dc6bbaf4_35d0_4fd9_aa86_d0c28461b16c",
            "/925/Culturalia_a54badb6_f788_442c_832d_ffa9ada0cb93",
            "/925/Culturalia_fc6edbc0_be0f_45f1_94e7_e7edf79ca785",
            "/925/Culturalia_723f92ae_4865_43de_8412_8a39b52a8450",
            "/925/Culturalia_9e2aaea2_6a1a_43b9_ae98_b60c3eab96c0",
            "/925/Culturalia_090e74f7_838a_41c2_88a5_ddbbbef62d28",
            "/925/Culturalia_4d202614_a884_42d6_934b_ef3186f0053a",
            "/925/Culturalia_4375465e_365f_48c8_ba9b_c7b793db2bed",
            "/925/Culturalia_7679413a_67d7_4b6b_8d22_7abc0306b970",
            "/925/Culturalia_b74b0fb2_e61e_4582_a64c_4b9071acf6a6",
            "/925/Culturalia_8a9a094a_3124_4b31_8e06_9996ff1b90c9",
            "/925/Culturalia_2884df85_dd83_48b8_b626_f73634833166",
            "/925/Culturalia_3e7e3837_8753_455f_9bf5_fca3fa263ed7",
            "/925/Culturalia_6d3ef102_4ff9_4ab0_9c04_16f8ed78a656",
            "/925/Culturalia_b174e5a2_453c_4c33_8e1d_09507d9861e2",
            "/925/Culturalia_d8aa88a1_7e0f_44ae_8969_8ec67cc26394",
            "/925/Culturalia_2f97be86_8e97_4b2b_b64e_284c16caa4b1",
            "/925/Culturalia_fb7b6977_dd90_453e_b474_173132001dcb",
            "/925/Culturalia_e9872b8a_6175_482c_a9b4_5392c0e5c4de",
            "/925/Culturalia_2347c988_565c_423f_9c01_331c32886629",
            "/925/Culturalia_cd9a9f2b_93bd_43cc_a941_3c1a74229cb1",
            "/925/Culturalia_d919dd3a_417e_4729_8189_c9837df7de4a",
            "/925/Culturalia_e6f83a89_7bc8_49f4_a6ff_8788f53651b9",
            "/925/Culturalia_a2c0bfec_cb2e_40b2_8e39_39b6ffdecb1f",
            "/925/Culturalia_1eb68c77_b7e8_433c_9226_1f3a62151207",
            "/925/Culturalia_08e33c73_cc9e_429e_9d69_bd018a3a7dad",
            "/925/Culturalia_5481bb77_2c5f_421b_9ae1_b6b1515117b2",
            "/925/Culturalia_548e8884_8f54_4415_bf66_be4a0238c6e4",
            "/925/Culturalia_65da3f15_0fc6_4207_b1c8_6a4f08f6b59d",
            "/925/Culturalia_ba2e5a35_aec5_4d24_bd0b_10699317186b",
            "/925/Culturalia_65244a8e_7342_4727_9cdf_1fbc8cfc9cec",
            "/925/Culturalia_90161528_a3d6_4daf_9282_0df713897f5e",
            "/925/Culturalia_788005db_1a19_414a_b42c_1a48cb2626a2",
            "/925/Culturalia_85d13fc8_1d98_4dd7_af07_50899ea5ec68",
            "/925/Culturalia_5b4e66fc_7072_4a82_8e6f_f83f1c301f9f",
            "/925/Culturalia_c82e0d02_f361_4c01_a2a4_1ab3bb3a0fc9",
            "/925/Culturalia_3834ce09_cf44_4f96_9461_1523b6bb84f1",
            "/925/Culturalia_0bcf9386_c64d_4c8d_88f7_316f79906c60",
            "/925/Culturalia_8280eaf1_cded_4199_884a_6b243802db81",
            "/925/Culturalia_029c650d_3a10_4803_9bbf_c6d801e8e1b4",
            "/925/Culturalia_928443a9_866b_453d_9c0c_fdbd20ab8c0c",
            "/925/Culturalia_ad2dcb38_3830_4379_95f0_2cb140e928d1",
            "/925/Culturalia_f225c04b_0a97_4cc0_9b09_ff8071669886",
            "/925/Culturalia_d91b906d_6cf0_4d8b_a3e9_d670d0d54512",
            "/925/Culturalia_8e3b6fb1_6076_4637_8116_a7102269053c",
            "/925/Culturalia_33bdeba0_2a0b_431d_8af6_0c985bae693e",
            "/925/Culturalia_bf2d44e0_d4c2_4ebb_af96_33c86b05646b",
            "/925/Culturalia_f6325bc4_ac70_40d3_a123_067523b3dde0",
            "/925/Culturalia_32104c88_9ef7_4937_944f_3c53183b42cb",
            "/925/Culturalia_9ce99395_5049_4f1e_ab51_e44d907f7d94",
            "/925/Culturalia_84c34c4d_44b1_4251_a5c9_40b333ed4cd7",
            "/925/Culturalia_35a0c25e_344c_4f91_b683_5a510173267f",
            "/925/Culturalia_9736bc6f_f730_4b09_947f_e9428274806b",
            "/925/Culturalia_c59e2643_b758_49eb_8c75_b8aa170280af",
            "/925/Culturalia_19f6e906_f622_4d35_b2e4_89be2a0a6189",
            "/925/Culturalia_187ef189_d5fd_4117_9aee_7de62d013056",
            "/925/Culturalia_daa1b06f_6357_4ecf_acf1_775415043102",
            "/925/Culturalia_7c4b12bf_ab09_458a_aae3_1a91a57480cb",
            "/925/Culturalia_5ca2fdf0_51f4_4523_8be2_8de78af87ba1",
            "/925/Culturalia_f3f852cb_cedb_4096_b690_f623f905a4ac",
            "/925/Culturalia_8334d5a4_764a_48d7_a382_4f828e44e3d6",
            "/925/Culturalia_1b31b906_04d5_485a_b730_8ea5a5bc07ea",
            "/925/Culturalia_9581be14_2f88_4bce_aaf9_0b521a5d10d4",
            "/925/Culturalia_0481edc3_7fd9_4d85_8ace_185e5d1d1b5a",
            "/925/Culturalia_25be3a59_9fe9_4e8a_8517_3c4a41c1e217",
            "/925/Culturalia_a2c1ad81_4bc5_4a28_afc8_06958bce5310",
            "/925/Culturalia_2d02ac04_34d0_4b49_9dd9_bec72f2cf663",
            "/925/Culturalia_51905ad0_8c56_42b8_8f4a_f7b106d79785",
            "/925/Culturalia_2a19ab8f_616d_4901_a929_45fd0428ffc7",
            "/925/Culturalia_c56678d7_8ad0_42cf_b979_2a93be76db0d",
            "/925/Culturalia_7f8fb01f_1b28_4311_99e4_9ca2515bf58f",
            "/925/Culturalia_9881ef5c_c5fd_4287_819b_5156f9a710ea",
            "/925/Culturalia_2675194b_e55f_4f82_b5bc_eea0c4c7672d",
            "/925/Culturalia_4c66050d_148e_42af_9201_67144ea139f1",
            "/925/Culturalia_1d510574_9e76_4bc8_9bc7_b2c0b4499d81",
            "/925/Culturalia_c01acf80_a39b_418d_9d4f_1055a5401169",
            "/925/Culturalia_57671d9e_2e37_4eda_a976_0294277e6a55",
            "/925/Culturalia_0c8cf293_fb20_4721_88e8_d378127d260a",
            "/925/Culturalia_ad85d0e1_d24f_4abc_ab64_bd7b82d524d9",
            "/925/Culturalia_b8a1eb67_8bca_4548_ac11_ebf0a9052aa2");

    public ItemReaderConfig(BatchRecordService batchRecordService, PostPublicationSettings postPublicationSettings) {
        this.batchRecordService = batchRecordService;
        this.postPublicationSettings = postPublicationSettings;
    }

    public SynchronizedItemStreamReader<FullBean> createRecordReader(Instant currentStartTime) {

        RecordDatabaseReader reader =
                new RecordDatabaseReader(
                        batchRecordService, postPublicationSettings.getBatchChunkSize(),
                        // Fetch record whose timestampUpdated is more than currentStartTime
                        Filters.gte("timestampUpdated", currentStartTime),
                        // TODO this is added for trial testing. Will be removed later
                        Filters.eq("europeanaCollectionName", "925_Ministerul_Culturii_UMP"),
                        Filters.in("about", records)
                );
        return threadSafeReader(reader);
    }

    /** Makes ItemReader thread-safe */
    private <T> SynchronizedItemStreamReader<T> threadSafeReader(ItemStreamReader<T> reader) {
        final SynchronizedItemStreamReader<T> synchronizedItemStreamReader =
                new SynchronizedItemStreamReader<>();
        synchronizedItemStreamReader.setDelegate(reader);
        return synchronizedItemStreamReader;
    }


}
