/*
 *    Copyright 2019-2024 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.backstage.app.attachment.service;

import com.backstage.app.attachment.configuration.properties.AttachmentProperties;
import com.backstage.app.attachment.model.AttachmentsAppStatusCode;
import com.backstage.app.attachment.model.domain.Attachment;
import com.backstage.app.attachment.model.domain.AttachmentBinding;
import com.backstage.app.attachment.repository.AttachmentBindingRepository;
import com.backstage.app.attachment.repository.AttachmentRepository;
import com.backstage.app.attachment.service.store.AttachmentStore;
import com.backstage.app.exception.AppException;
import com.backstage.app.utils.transactional.TransactionalUtils;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(AttachmentProperties.ACTIVATION_PROPERTY)
@RequiredArgsConstructor
public class AttachmentService
{
	private final AttachmentRepository attachmentRepository;
	private final AttachmentBindingRepository attachmentBindingRepository;

	@Getter
	private final List<AttachmentStore> attachmentStores;

	@Getter
	private AttachmentStore attachmentStore;

	@Getter
	private final List<AttachmentServiceAdvice> serviceAdviceList;

	private final AttachmentProperties attachmentProperties;

	@PostConstruct
	public void initialize()
	{
		var storeType = attachmentProperties.getStoreType();

		log.info("Primary attachment store type: {}.", storeType);

		attachmentStore = getAttachmentStore(storeType);
	}

	public Resource getAttachmentData(String attachmentId)
	{
		serviceAdviceList.forEach(it -> it.handleGetAttachmentData(attachmentId));

		return attachmentStore.getAttachment(attachmentRepository.findByIdEx(attachmentId));
	}

	public Attachment getAttachment(@NotNull String id)
	{
		serviceAdviceList.forEach(advice -> advice.handleGetAttachment(id));

		return attachmentRepository.findByIdEx(id);
	}

	public List<Attachment> getAttachments(@NotNull List<String> ids)
	{
		serviceAdviceList.forEach(advice -> advice.handleGetAttachments(ids));

		return attachmentRepository.findAll(ids);
	}

	public List<Attachment> getAttachments(@NotNull Enum<?> type, @NotNull String objectId)
	{
		return getAttachments(type.name(), objectId);
	}

	public List<Attachment> getAttachments(@NotNull String type, @NotNull String objectId)
	{
		var attachments = attachmentBindingRepository.findAttachmentsByTypeAndObjectId(type, objectId);

		attachments.forEach(attachment -> serviceAdviceList.forEach(it -> it.handleGetAttachment(attachment.getId())));

		return attachments;
	}

	public Map<String, List<Attachment>> getAttachments(@NotNull Enum<?> type, @NotNull Collection<String> objectIds)
	{
		return getAttachments(type.name(), objectIds);
	}

	public Map<String, List<Attachment>> getAttachments(@NotNull String type, @NotNull Collection<String> objectIds)
	{
		var attachmentsByObjectId = attachmentBindingRepository.findAttachmentsByTypeAndObjectIds(type, objectIds);

		attachmentsByObjectId.values().forEach(attachments -> {
			attachments.forEach(attachment -> serviceAdviceList.forEach(it -> it.handleGetAttachment(attachment.getId())));
		});

		return attachmentsByObjectId;
	}

	public List<String> getAttachmentIds(@NotNull Enum<?> type, @NotNull String objectId)
	{
		return attachmentBindingRepository.findAttachmentIdsByTypeAndObjectId(type.name(), objectId);
	}

	public List<String> getAttachmentIds(@NotNull String type, @NotNull String objectId)
	{
		return attachmentBindingRepository.findAttachmentIdsByTypeAndObjectId(type, objectId);
	}

	@Transactional
	public void bindAttachment(@NotNull String attachmentId, @NotNull String userId, @NotNull Enum<?> type, @NotNull String objectId)
	{
		bindAttachment(attachmentId, userId, type.name(), objectId);
	}

	@Transactional
	public void bindAttachment(@NotNull String attachmentId, @NotNull String userId, @NotNull String type, @NotNull String objectId)
	{
		serviceAdviceList.forEach(advice -> advice.handleBindAttachment(attachmentId, userId, type, objectId));

		Attachment attachment = attachmentRepository.findByIdEx(attachmentId);

		attachmentRepository.getEntityManager().refresh(attachment);

		for (AttachmentBinding binding : attachment.getBindings())
		{
			if (binding.getUserId().equals(userId) && binding.getType().equals(type) && binding.getObjectId().equals(objectId))
			{
				return;
			}
		}

		var attachmentBinding = new AttachmentBinding();
		attachmentBinding.setType(type);
		attachmentBinding.setObjectId(objectId);
		attachmentBinding.setCreated(LocalDateTime.now());
		attachmentBinding.setAttachment(attachment);
		attachmentBinding.setUserId(userId);

		attachment.getBindings().add(attachmentBinding);

		attachmentBindingRepository.saveAndFlush(attachmentBinding);
	}

	@Transactional
	public void bindAttachments(@NonNull Collection<String> attachmentIds, @NonNull String userId, @NonNull Enum<?> type, @NonNull String objectIds)
	{
		bindAttachments(attachmentIds, userId, type.name(), objectIds);
	}

	@Transactional
	public void bindAttachments(@NonNull Collection<String> attachmentIds, @NonNull String userId, @NonNull String type, @NonNull String objectId)
	{
		attachmentIds.forEach(attachmentId -> bindAttachment(attachmentId, userId, type, objectId));
	}

	@Transactional
	public void releaseAttachment(@NonNull String attachmentId)
	{
		attachmentBindingRepository.deleteByAttachmentId(attachmentId);

		attachmentBindingRepository.flush();
	}

	@Transactional
	public void releaseAttachments(@NonNull Collection<String> attachmentIds, @NonNull String userId, @NonNull String type, @NonNull String objectId)
	{
		attachmentIds.forEach(id -> attachmentBindingRepository.deleteByAttachmentIdAndUserIdAndTypeAndObjectId(id, userId, type, objectId));

		attachmentBindingRepository.flush();
	}

	@Transactional
	public void releaseAttachment(@NonNull String attachmentId, @NonNull String userId, @NonNull Enum<?> type, @NonNull String objectId)
	{
		releaseAttachment(attachmentId, userId, type.name(), objectId);
	}

	@Transactional
	public void releaseAttachment(@NonNull String attachmentId, @NonNull String userId, @NonNull String type, @NonNull String objectId)
	{
		attachmentBindingRepository.deleteByAttachmentIdAndUserIdAndTypeAndObjectId(attachmentId, userId, type, objectId);

		attachmentBindingRepository.flush();
	}

	@Transactional
	public void releaseAttachments(@NotNull Enum<?> type, @NotNull String objectId)
	{
		releaseAttachments(type.name(), objectId);
	}

	@Transactional
	public void releaseAttachments(@NotNull String type, @NotNull String objectId)
	{
		attachmentBindingRepository.deleteByTypeAndObjectId(type, objectId);

		attachmentBindingRepository.flush();
	}

	public boolean isAttachmentBound(@NonNull String attachmentId, @NonNull Enum<?> type)
	{
		return isAttachmentBound(attachmentId, type.name());
	}

	public boolean isAttachmentBound(@NonNull String attachmentId, @NonNull String type)
	{
		return attachmentBindingRepository.existsByAttachmentIdAndType(attachmentId, type);
	}

	@Transactional
	public Attachment addAttachment(@NonNull String fileName, @NonNull String mimeType, @NonNull String userId, byte[] data)
	{
		return addAttachment(null, fileName, mimeType, userId, new ByteArrayInputStream(data));
	}

	@Transactional
	public Attachment addAttachment(@NonNull String fileName, @NonNull String mimeType, @NonNull String userId, InputStream stream)
	{
		return addAttachment(null, fileName, mimeType, userId, stream);
	}

	@Transactional
	public Attachment addAttachment(String id, @NonNull String fileName, @NonNull String mimeType, @NonNull String userId, InputStream stream)
	{
		var attachment = attachmentRepository.save(Attachment.builder()
				.id(id)
				.userId(userId)
				.created(LocalDateTime.now())
				.fileName(fileName)
				.mimeType(mimeType)
				.build());

		var resource = attachmentStore.saveAttachment(attachment, stream);

		TransactionalUtils.doOnRollback(() -> attachmentStore.deleteAttachment(attachment));

		try (var resourceStream = resource.getInputStream())
		{
			attachment.setSize(resource.contentLength());
			attachment.setChecksum(calculateChecksum(resourceStream));
		}
		catch (IOException e)
		{
			throw new AppException(ApiStatusCodeImpl.ATTACHMENT_ADD_ERROR, e);
		}

		serviceAdviceList.forEach(advice -> advice.handleAddAttachment(id, fileName, mimeType, userId, attachmentStore.getAttachment(attachment)));

		return attachmentRepository.saveAndFlush(attachment);
	}

	@Transactional
	public Attachment updateAttachment(@NonNull String id, @NonNull String fileName, @NonNull String mimeType, @NonNull String userId, byte[] data)
	{
		return addAttachment(id, fileName, mimeType, userId, new ByteArrayInputStream(data));
	}

	@Transactional
	public Attachment updateAttachment(@NonNull String id, @NonNull String fileName, @NonNull String mimeType, @NonNull String userId, InputStream stream)
	{
		var attachment = getAttachment(id);
		attachment.setUpdated(LocalDateTime.now());
		attachment.setFileName(fileName);
		attachment.setMimeType(mimeType);
		attachment.setUserId(userId);

		var resource = attachmentStore.saveAttachment(attachment, stream);

		try (var resourceStream = resource.getInputStream())
		{
			attachment.setSize(resource.contentLength());
			attachment.setChecksum(calculateChecksum(resourceStream));
		}
		catch (IOException e)
		{
			throw new AppException(ApiStatusCodeImpl.ATTACHMENT_ADD_ERROR, e);
		}

		return attachment;
	}

	public void syncAttachmentStores(AttachmentProperties.StoreType source, AttachmentProperties.StoreType target)
	{
		if (source == target)
		{
			throw new AppException(AttachmentsAppStatusCode.ATTACHMENT_STORE_SYNC_ERROR, "Исходное и целевое хранилища совпадают.");
		}

		log.info("Синхронизируем содержимое хранилищ вложений {} -> {}.", source, target);

		var sourceStore = getAttachmentStore(source);
		var targetStore = getAttachmentStore(target);

		var attachmentIds = attachmentRepository.findAllIds();

		log.info("Будет перенесено вложений: {}.", attachmentIds.size());

		int counter = 0;

		for (var attachmentId : attachmentIds)
		{
			counter++;

			var attachment = getAttachment(attachmentId);

			if (sourceStore.attachmentExists(attachment) && !targetStore.attachmentExists(attachment))
			{
				targetStore.saveAttachment(attachment, sourceStore.getAttachment(attachment));
			}

			if (counter % 100 == 0)
			{
				log.info("Обработано {} из {} вложений.", counter, attachmentIds.size());
			}
		}

		log.info("Хранилища синхронизированы.");
	}

	public String calculateChecksum(InputStream stream)
	{
		try
		{
			return DigestUtils.md5Hex(stream).toUpperCase();
		}
		catch (Exception e)
		{
			throw new AppException(ApiStatusCodeImpl.ATTACHMENT_STORE_ERROR, "При вычислении контрольной суммы вложения произошла ошибка.");
		}
	}

	public AttachmentStore getAttachmentStore(AttachmentProperties.StoreType storeType)
	{
		return attachmentStores.stream().filter(it -> storeType.equals(it.getType())).findFirst().orElseThrow(
				() -> new AppException(AttachmentsAppStatusCode.ATTACHMENT_STORE_INIT_FAILED, "Не доступно хранилище вложений с типом '%s'.".formatted(storeType))
		);
	}

	@Transactional
	public void deleteAttachment(String attachmentId)
	{
		var attachment = attachmentRepository.findByIdEx(attachmentId);

		attachmentStore.deleteAttachment(attachment);
		attachmentRepository.delete(attachment);
	}
}
