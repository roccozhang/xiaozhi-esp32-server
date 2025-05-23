package xiaozhi.modules.agent.service.biz.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xiaozhi.modules.agent.dto.AgentChatHistoryReportDTO;
import xiaozhi.modules.agent.entity.AgentChatHistoryEntity;
import xiaozhi.modules.agent.entity.AgentEntity;
import xiaozhi.modules.agent.service.AgentChatAudioService;
import xiaozhi.modules.agent.service.AgentChatHistoryService;
import xiaozhi.modules.agent.service.AgentService;
import xiaozhi.modules.agent.service.biz.AgentChatHistoryBizService;

/**
 * {@link AgentChatHistoryBizService} impl
 *
 * @author Goody
 * @version 1.0, 2025/4/30
 * @since 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AgentChatHistoryBizServiceImpl implements AgentChatHistoryBizService {
    private final AgentService agentService;
    private final AgentChatHistoryService agentChatHistoryService;
    private final AgentChatAudioService agentChatAudioService;

    /**
     * 处理聊天记录上报，包括文件上传和相关信息记录
     *
     * @param report 包含聊天上报所需信息的输入对象
     * @return 上传结果，true表示成功，false表示失败
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean report(AgentChatHistoryReportDTO report) {
        String macAddress = report.getMacAddress();
        Byte chatType = report.getChatType();
        log.info("小智设备聊天上报请求: macAddress={}, type={}", macAddress, chatType);

        // 1. base64解码report.getOpusDataBase64(),存入ai_agent_chat_audio表
        String audioId = null;
        if (report.getOpusDataBase64() != null && !report.getOpusDataBase64().isEmpty()) {
            try {
                // TODO: 需要考虑保留什么格式的音频数据，比如是opus还是wave
                // byte[] audioData = Base64.getDecoder().decode(report.getOpusDataBase64());
                // audioId = agentChatAudioService.saveAudio(audioData);
                // log.info("音频数据保存成功，audioId={}", audioId);
            } catch (Exception e) {
                log.error("音频数据保存失败", e);
                return false;
            }
        }

        // 2. 组装上报数据
        // 2.1 根据设备MAC地址查询对应的默认智能体，判断是否需要上报
        AgentEntity agentEntity = agentService.getDefaultAgentByMacAddress(macAddress);
        if (agentEntity == null) {
            return false;
        }
        String agentId = agentEntity.getId();
        log.info("设备 {} 对应智能体 {} 上报", macAddress, agentEntity.getId());

        // 2.2 构建聊天记录实体
        AgentChatHistoryEntity entity = AgentChatHistoryEntity.builder()
                .macAddress(macAddress)
                .agentId(agentId)
                .sessionId(report.getSessionId())
                .chatType(report.getChatType())
                .content(report.getContent())
                .audioId(audioId)
                .build();

        // 3. 保存数据
        agentChatHistoryService.save(entity);
        return Boolean.TRUE;
    }
}
